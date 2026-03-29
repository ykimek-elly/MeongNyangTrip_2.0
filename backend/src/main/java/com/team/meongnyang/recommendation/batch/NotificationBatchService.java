package com.team.meongnyang.recommendation.batch;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.log.RecommendationBatchTraceContext;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.notification.service.NotificationService;
import com.team.meongnyang.recommendation.service.RecommendationPipelineService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * 추천 알림 배치를 실행한다.
 *
 * <p>발송 대상 조회, 대표 반려동물 조회, 정책 검증, 추천 생성, 알림 발송,
 * 발송 이력 저장을 한 흐름으로 묶는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final RecommendationPipelineService recommendationPipelineService;
    private final NotificationService notificationService;
    private final DailyRecommendationCacheService dailyRecommendationCacheService;
    private final NotificationPolicyService notificationPolicyService;

    @Value("${batch.notification-parallelism:4}")
    private int notificationParallelism;

    /**
     * 전체 추천 알림 배치를 실행한다.
     */
    public void runDailyNotificationBatch() {
        String batchExecutionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("[NotificationBatch] 시작 batchExecutionId={}", batchExecutionId);

        List<User> targets = getNotificationTargets();
        Map<Long, Pet> representativePetMap = loadRepresentativePets(targets);
        BatchExecutionSummary summary = new BatchExecutionSummary();

        if (!targets.isEmpty()) {
            int maxConcurrency = Math.max(1, Math.min(notificationParallelism, targets.size()));
            Semaphore semaphore = new Semaphore(maxConcurrency);
            List<Future<?>> futures = new ArrayList<>(targets.size());

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (User target : targets) {
                    semaphore.acquireUninterruptibly();
                    futures.add(executor.submit(() -> {
                        try {
                            processTarget(
                                    batchExecutionId,
                                    target,
                                    representativePetMap.get(target.getUserId()),
                                    summary
                            );
                        } finally {
                            semaphore.release();
                        }
                    }));
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        summary.fail(NotificationBatchFailureReason.UNKNOWN_ERROR);
                        log.error("[NotificationBatch] future 처리 중 예외가 발생했습니다. batchExecutionId={}", batchExecutionId, e);
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("[NotificationBatch] 종료 batchExecutionId={}, total={}, success={}, fail={}, elapsedMs={}",
                batchExecutionId,
                targets.size(),
                summary.getSuccessCount(),
                summary.getFailureCount(),
                endTime - startTime);
        log.info("[NotificationBatch] 실패 요약 batchExecutionId={}, reasons={}",
                batchExecutionId,
                summary.formatFailureReasons());
    }

    /**
     * 알림 수신이 가능한 사용자 목록을 조회한다.
     *
     * @return 배치 대상 사용자 목록
     */
    public List<User> getNotificationTargets() {
        return userRepository.findAllByNotificationEnabledTrueAndStatusAndRole(
                User.Status.ACTIVE,
                User.Role.USER
        );
    }

    /**
     * 대상 사용자들의 대표 반려동물을 한 번에 조회한다.
     *
     * @param targets 사용자 목록
     * @return userId 기준 대표 반려동물 맵
     */
    private Map<Long, Pet> loadRepresentativePets(List<User> targets) {
        if (targets.isEmpty()) {
            return Map.of();
        }

        List<Long> userIds = targets.stream()
                .map(User::getUserId)
                .collect(Collectors.toList());

        return petRepository.findAllByUserUserIdInAndIsRepresentativeTrueAndUserStatusAndUserRole(
                        userIds,
                        User.Status.ACTIVE,
                        User.Role.USER
                ).stream()
                .collect(Collectors.toMap(
                        pet -> pet.getUser().getUserId(),
                        pet -> pet,
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    /**
     * 사용자 한 명에 대한 배치 처리 전체를 수행한다.
     *
     * @param batchExecutionId 배치 실행 ID
     * @param target 사용자
     * @param pet 대표 반려동물
     * @param summary 집계 객체
     */
    private void processTarget(
            String batchExecutionId,
            User target,
            Pet pet,
            BatchExecutionSummary summary
    ) {
        try (RecommendationBatchTraceContext.TraceScope ignored =
                     RecommendationBatchTraceContext.open(batchExecutionId, target.getUserId(), pet == null ? null : pet.getPetId())) {

            if (pet == null) {
                summary.fail(NotificationBatchFailureReason.PET_NOT_FOUND);
                log.warn("[NotificationBatch] 대표 반려동물이 없습니다. userId={}", target.getUserId());
                return;
            }

            NotificationPolicyService.NotificationDecision decision = notificationPolicyService.evaluate(
                    target,
                    pet,
                    LocalDateTime.now(),
                    dailyRecommendationCacheService.isSentToday(target.getUserId(), target.getLastNotificationSentAt())
            );
            if (!decision.isSend()) {
                log.info("[NotificationBatch] 정책에 의해 발송을 건너뜁니다. userId={}, reason={}",
                        target.getUserId(),
                        decision.getReason());
                return;
            }

            RecommendationNotificationResult recommendationResult =
                    recommendationPipelineService.recommendForNotification(target, pet, batchExecutionId);

            Place topPlace = recommendationResult.getPlace();
            if (topPlace == null) {
                NotificationBatchFailureReason failureReason = resolveNoCandidateReason(recommendationResult);
                summary.fail(failureReason);
                log.warn("[NotificationBatch] 추천 결과가 없습니다. userId={}, petId={}, reason={}",
                        target.getUserId(),
                        pet.getPetId(),
                        failureReason);
                return;
            }

            NotificationResponse notificationResponse = notificationService.send(
                    target,
                    pet,
                    topPlace,
                    recommendationResult.getMessage(),
                    recommendationResult.getWeatherType()
            );

            if (notificationResponse != null && notificationResponse.isSuccess()) {
                LocalDateTime sentAt = LocalDateTime.now();
                target.markNotificationSent(sentAt);
                userRepository.save(target);
                dailyRecommendationCacheService.saveToday(target.getUserId(), recommendationResult, batchExecutionId);

                summary.success();
                log.info("[NotificationBatch] 발송 성공 userId={}, petId={}, placeId={}, code={}",
                        target.getUserId(),
                        pet.getPetId(),
                        topPlace.getId(),
                        notificationResponse.getStatusCode());
                return;
            }

            NotificationBatchFailureReason failureReason = resolveNotificationFailureReason(notificationResponse);
            summary.fail(failureReason);
            log.warn("[NotificationBatch] 발송 실패 userId={}, petId={}, placeId={}, reason={}, code={}",
                    target.getUserId(),
                    pet.getPetId(),
                    topPlace.getId(),
                    failureReason,
                    notificationResponse == null ? null : notificationResponse.getStatusCode());
        } catch (BusinessException e) {
            NotificationBatchFailureReason failureReason = resolveBusinessFailureReason(e);
            summary.fail(failureReason);
            log.error("[NotificationBatch] 비즈니스 예외 userId={}, reason={}",
                    target.getUserId(),
                    failureReason,
                    e);
        } catch (Exception e) {
            NotificationBatchFailureReason failureReason = resolveExceptionFailureReason(e);
            summary.fail(failureReason);
            log.error("[NotificationBatch] 처리 중 예외 userId={}, reason={}",
                    target.getUserId(),
                    failureReason,
                    e);
        }
    }

    /**
     * 추천 결과가 비어 있을 때 실패 사유를 분류한다.
     *
     * @param recommendationResult 추천 결과
     * @return 실패 사유
     */
    private NotificationBatchFailureReason resolveNoCandidateReason(RecommendationNotificationResult recommendationResult) {
        if (recommendationResult != null
                && "ERROR".equalsIgnoreCase(recommendationResult.getWeatherWalkLevel())) {
            return NotificationBatchFailureReason.WEATHER_API_ERROR;
        }
        return NotificationBatchFailureReason.NO_CANDIDATE;
    }

    /**
     * 알림 응답으로부터 실패 사유를 분류한다.
     *
     * @param notificationResponse 알림 응답
     * @return 실패 사유
     */
    private NotificationBatchFailureReason resolveNotificationFailureReason(NotificationResponse notificationResponse) {
        if (notificationResponse == null) {
            return NotificationBatchFailureReason.NOTIFICATION_SEND_FAIL;
        }
        if ("INVALID_REQUEST".equalsIgnoreCase(notificationResponse.getStatusCode())) {
            return NotificationBatchFailureReason.NOTIFICATION_MESSAGE_BUILD_FAIL;
        }
        return NotificationBatchFailureReason.NOTIFICATION_SEND_FAIL;
    }

    /**
     * 비즈니스 예외를 실패 사유로 변환한다.
     *
     * @param e 비즈니스 예외
     * @return 실패 사유
     */
    private NotificationBatchFailureReason resolveBusinessFailureReason(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode == ErrorCode.USER_NOT_FOUND) {
            return NotificationBatchFailureReason.USER_NOT_FOUND;
        }
        if (errorCode == ErrorCode.PET_NOT_FOUND) {
            return NotificationBatchFailureReason.PET_NOT_FOUND;
        }
        return NotificationBatchFailureReason.UNKNOWN_ERROR;
    }

    /**
     * 일반 예외를 유형별 실패 사유로 분류한다.
     *
     * @param e 예외
     * @return 실패 사유
     */
    private NotificationBatchFailureReason resolveExceptionFailureReason(Exception e) {
        String message = e.getMessage();
        String exceptionType = e.getClass().getName();

        if ((message != null && message.toLowerCase().contains("weather"))
                || exceptionType.contains(".weather.")) {
            return NotificationBatchFailureReason.WEATHER_API_ERROR;
        }

        if ((message != null && message.toLowerCase().contains("gemini"))
                || exceptionType.contains("Gemini")
                || exceptionType.contains(".ai.")) {
            return NotificationBatchFailureReason.AI_RESPONSE_ERROR;
        }

        return NotificationBatchFailureReason.UNKNOWN_ERROR;
    }

    /**
     * 배치 실행 집계를 담는 내부 객체다.
     */
    private static class BatchExecutionSummary {
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final EnumMap<NotificationBatchFailureReason, LongAdder> failureReasonCounts =
                new EnumMap<>(NotificationBatchFailureReason.class);

        private BatchExecutionSummary() {
            for (NotificationBatchFailureReason reason : NotificationBatchFailureReason.values()) {
                failureReasonCounts.put(reason, new LongAdder());
            }
        }

        private void success() {
            successCount.increment();
        }

        private void fail(NotificationBatchFailureReason reason) {
            failureCount.increment();
            failureReasonCounts.get(reason).increment();
        }

        private long getSuccessCount() {
            return successCount.sum();
        }

        private long getFailureCount() {
            return failureCount.sum();
        }

        private Map<NotificationBatchFailureReason, Long> formatFailureReasons() {
            EnumMap<NotificationBatchFailureReason, Long> formatted = new EnumMap<>(NotificationBatchFailureReason.class);
            failureReasonCounts.forEach((reason, count) -> {
                long value = count.sum();
                if (value > 0) {
                    formatted.put(reason, value);
                }
            });
            return formatted;
        }
    }
}
