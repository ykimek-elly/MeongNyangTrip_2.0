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
 * 추천 알림 배치를 실행하는 서비스
 *
 * <p>알림 수신 동의한 활성 사용자 목록을 조회하고
 * 사용자별 대표 반려동물 기준으로 추천 파이프라인을 호출한 뒤
 * 추천 결과를 카카오 알림 메시지로 전송한다.</p>
 *
 * <p>배치 성공 시 lastNotificationSentAt 을 갱신하고,
 * daily recommendation cache를 저장한다.</p>
 *
 * <p>병렬 처리를 위해 Virtual Thread 기반 Executor를 사용하고,
 * 동시 실행 수는 Semaphore로 제한한다.</p>
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

  // Semaphore 값 반영
  @Value("${batch.notification-parallelism:4}")
  private int notificationParallelism;

  /**
   * 전체 추천 알림 배치를 실행한다.
   *
   * <p>배치 실행 ID를 생성하고 대상 사용자를 조회한 뒤
   * Virtual Thread 기반 병렬 처리로 사용자별 추천/알림을 수행한다.</p>
   */
  public void runDailyNotificationBatch() {
    String batchExecutionId = UUID.randomUUID().toString();
    long startTime = System.currentTimeMillis();
    log.info("[NotificationBatch] start batchExecutionId={}", batchExecutionId);

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
            log.error("[NotificationBatch] future error batchExecutionId={}, reason={}, error={}",
                    batchExecutionId,
                    NotificationBatchFailureReason.UNKNOWN_ERROR,
                    e.getMessage(),
                    e);
          }
        }
      }
    }

    long endTime = System.currentTimeMillis();
    log.info("[NotificationBatch] finished batchExecutionId={}, total={}, success={}, fail={}, elapsedMs={}",
            batchExecutionId,
            targets.size(),
            summary.getSuccessCount(),
            summary.getFailureCount(),
            endTime - startTime);
    log.info("[NotificationBatch] failure summary batchExecutionId={}, reasons={}",
            batchExecutionId,
            summary.formatFailureReasons());
  }

  /**
   * 알림 수신이 활성화된 사용자 목록을 조회한다.
   */
  public List<User> getNotificationTargets() {
    return userRepository.findAllByNotificationEnabledTrueAndStatusAndRole(
            User.Status.ACTIVE,
            User.Role.USER
    );
  }

  /**
   * 배치 대상 사용자들의 대표 반려동물을 한 번에 조회해 Map 형태로 반환한다.
   *
   * <p>userId를 key로 사용해 이후 추천 처리 시 빠르게 참조한다.</p>
   */
  private Map<Long, Pet> loadRepresentativePets(List<User> targets) {
    if (targets.isEmpty()) {
      return Map.of();
    }

    List<Long> userIds = targets.stream()
            .map(User::getUserId)
            .collect(Collectors.toList());

    return petRepository.findAllByUserUserIdInAndIsRepresentativeTrueAndUserStatusAndUserRole(userIds, User.Status.ACTIVE, User.Role.USER).stream()
            .collect(Collectors.toMap(
                    pet -> pet.getUser().getUserId(),
                    pet -> pet,
                    (left, right) -> left,
                    HashMap::new
            ));
  }

  /**
   * 단일 사용자에 대해 추천 생성, 메시지 생성, 알림 전송까지 처리한다.
   *
   * <p>전송 실패 건은 reason 기준으로 집계한다.</p>
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
        log.warn("[NotificationBatch] batch failed userId={}, reason={}",
                target.getUserId(),
                NotificationBatchFailureReason.PET_NOT_FOUND);
        return;
      }

      RecommendationNotificationResult recommendationResult =
              recommendationPipelineService.recommendForNotification(target, pet, batchExecutionId);

      Place topPlace = recommendationResult.getPlace();
      if (topPlace == null) {
        NotificationBatchFailureReason failureReason = resolveNoCandidateReason(recommendationResult);
        summary.fail(failureReason);
        log.warn("[NotificationBatch] batch failed userId={}, petId={}, reason={}",
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
        target.markNotificationSent();
        userRepository.save(target);
        dailyRecommendationCacheService.saveToday(target.getUserId(), recommendationResult, batchExecutionId);

        summary.success();
        log.info("[NotificationBatch] success userId={}, petId={}, placeId={}, code={}",
                target.getUserId(),
                pet.getPetId(),
                topPlace.getId(),
                notificationResponse.getStatusCode());
        return;
      }

      NotificationBatchFailureReason failureReason = resolveNotificationFailureReason(notificationResponse);
      summary.fail(failureReason);
      log.warn("[NotificationBatch] batch failed userId={}, petId={}, placeId={}, reason={}, code={}, message={}",
              target.getUserId(),
              pet.getPetId(),
              topPlace.getId(),
              failureReason,
              notificationResponse.getStatusCode(),
              notificationResponse.getStatusName());
    } catch (BusinessException e) {
      NotificationBatchFailureReason failureReason = resolveBusinessFailureReason(e);
      summary.fail(failureReason);
      log.error("[NotificationBatch] target error userId={}, reason={}, error={}",
              target.getUserId(),
              failureReason,
              e.getMessage(),
              e);
    } catch (Exception e) {
      NotificationBatchFailureReason failureReason = resolveExceptionFailureReason(e);
      summary.fail(failureReason);
      log.error("[NotificationBatch] target error userId={}, reason={}, error={}",
              target.getUserId(),
              failureReason,
              e.getMessage(),
              e);
    }
  }

  private NotificationBatchFailureReason resolveNoCandidateReason(RecommendationNotificationResult recommendationResult) {
    if (recommendationResult != null
            && "ERROR".equalsIgnoreCase(recommendationResult.getWeatherWalkLevel())) {
      return NotificationBatchFailureReason.WEATHER_API_ERROR;
    }
    return NotificationBatchFailureReason.NO_CANDIDATE;
  }

  private NotificationBatchFailureReason resolveNotificationFailureReason(NotificationResponse notificationResponse) {
    if (notificationResponse == null) {
      return NotificationBatchFailureReason.NOTIFICATION_SEND_FAIL;
    }

    if ("INVALID_REQUEST".equalsIgnoreCase(notificationResponse.getStatusCode())) {
      return NotificationBatchFailureReason.NOTIFICATION_MESSAGE_BUILD_FAIL;
    }

    return NotificationBatchFailureReason.NOTIFICATION_SEND_FAIL;
  }

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
