package com.team.meongnyang.batch.notification;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.log.RecommendationBatchTraceContext;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.notification.service.KakaoNotificationService;
import com.team.meongnyang.recommendation.notification.service.NotificationMessageBuilder;
import com.team.meongnyang.recommendation.service.RecommendationPipelineService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
 * 추천 알림 배치를 실행하는 서비스.
 *
 * <p>알림 수신 동의한 활성 사용자 목록을 조회한 뒤,
 * 사용자별 대표 반려동물을 기준으로 추천 파이프라인을 호출하고
 * 추천 결과를 카카오 알림 메시지로 전송한다.</p>
 *
 * <p>배치 성공 시 lastNotificationSentAt을 갱신하고,
 * daily recommendation cache를 저장한다.</p>
 *
 * <p>병렬 처리를 위해 Virtual Thread 기반 Executor를 사용하며,
 * 동시 실행 수는 Semaphore로 제한한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {
  private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

  private final UserRepository userRepository;
  private final PetRepository petRepository;
  private final RecommendationPipelineService recommendationPipelineService;
  private final KakaoNotificationService kakaoNotificationService;
  private final NotificationMessageBuilder notificationMessageBuilder;
  private final DailyRecommendationCacheService dailyRecommendationCacheService;

  // Semaphore Yml 값 반영
  @Value("${batch.notification-parallelism:4}")
  private int notificationParallelism;

  /**
   * 전체 추천 알림 배치를 실행한다.
   *
   * <p>배치 실행 ID를 생성하고 대상 사용자를 조회한 뒤,
   * Virtual Thread 기반 병렬 처리로 사용자별 추천/알림을 수행한다.</p>
   */
  public void runDailyNotificationBatch() {
    // 배치 실행 단위를 추적하기 위해 고유 실행 ID 생성
    String batchExecutionId = UUID.randomUUID().toString();
    long startTime = System.currentTimeMillis();
    log.info("[NotificationBatch] start batchExecutionId={}", batchExecutionId);

    // 알림 대상 사용자와 대표 반려동물 사전 조회
    List<User> targets = getNotificationTargets();
    Map<Long, Pet> representativePetMap = loadRepresentativePets(targets);

    // 여러 스레드에서 동시에 카운터를 증가/감소해야 하므로 충돌을 방지하기 위해 LongAdder 사용
    LongAdder successCount = new LongAdder();
    LongAdder failureCount = new LongAdder();
    LongAdder skipCount = new LongAdder();

    // 알림 활성화 된 사용자가 있을 때
    if (!targets.isEmpty()) {
      // 실행 개수 설정
      int maxConcurrency = Math.max(1, Math.min(notificationParallelism, targets.size()));
      // 동시 실행 개수 제한
      Semaphore semaphore = new Semaphore(maxConcurrency);
      // 비동기 작업 결과 핸들링, 병렬 작업 예외 수집과 완료 보장을 위해 사용
      List<Future<?>> futures = new ArrayList<>(targets.size());

      // Virtual Thread 기반 병렬 처리
      try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (User target : targets) {
          // 동시 실행 제한을 안정적으로 적용
          semaphore.acquireUninterruptibly();
          // 사용자별 추천/알림 처리 작업 제출
          futures.add(executor.submit(() -> {
            try {
              processTarget(
                      batchExecutionId,
                      target,
                      representativePetMap.get(target.getUserId()),
                      successCount,
                      failureCount,
                      skipCount
              );
            } finally {
              semaphore.release();
            }
          }));
        }

        // 제출된 작업 완료 대기 및 예외 수집
        for (Future<?> future : futures) {
          try {
            // 끝날 때까지 기다림
            future.get();
          } catch (Exception e) {
            failureCount.increment();
            log.error("[NotificationBatch] future error batchExecutionId={}, error={}",
                    batchExecutionId,
                    e.getMessage(),
                    e);
          }
        }
      }
    }

    long endTime = System.currentTimeMillis();
    log.info("[NotificationBatch] finished batchExecutionId={}, total={}, success={}, failure={}, skip={}, elapsedMs={}",
            batchExecutionId,
            targets.size(),
            successCount.sum(),
            failureCount.sum(),
            skipCount.sum(),
            endTime - startTime);
  }

  /**
   * 알림 수신이 활성화된 사용자 목록을 조회한다.
   */
  public List<User> getNotificationTargets() {
    return userRepository.findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE);
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

    // 대상 사용자 ID 목록 추출
    List<Long> userIds = targets.stream()
            .map(User::getUserId)
            .collect(Collectors.toList());

    // 대표 반려동물을 userId 기준 Map으로 변환
    return petRepository.findAllByUserUserIdInAndIsRepresentativeTrue(userIds).stream()
            // userId를 key로 사용, 동일한 userId일 경우 left 값을 사용
            .collect(Collectors.toMap(
                    pet -> pet.getUser().getUserId(),
                    pet -> pet,
                    (left, right) -> left,
                    HashMap::new
            ));
  }

  /**
   * 단일 사용자에 대해 추천 생성 → 메시지 생성 → 알림 전송까지 처리한다.
   *
   * <p>추천 결과가 없거나 대표 반려동물이 없으면 skip 처리한다.</p>
   *
   * <p>전송 성공 시:
   * - lastNotificationSentAt 갱신
   * - daily recommendation cache 저장</p>
   */
  private void processTarget(
          String batchExecutionId,
          User target,
          Pet pet,
          LongAdder successCount,
          LongAdder failureCount,
          LongAdder skipCount
  ) {
    // 사용자 단위 trace context 시작
    // 로그를 사용자 단위로 묶기 위해 사용
    try (RecommendationBatchTraceContext.TraceScope ignored =
                 RecommendationBatchTraceContext.open(batchExecutionId, target.getUserId(), pet == null ? null : pet.getPetId())) {
      // 대표 반려동물이 없으면 추천 대상 제외
      if (pet == null) {
        skipCount.increment();
        log.info("[NotificationBatch] skip userId={}, reason=no representative pet", target.getUserId());
        return;
      }
      // 추천 파이프라인 호출
      RecommendationNotificationResult recommendationResult =
              recommendationPipelineService.recommendForNotification(target, pet, batchExecutionId);

      Place topPlace = recommendationResult.getPlace();
      // 추천 결과가 없으면 알림 생략
      if (topPlace == null) {
        skipCount.increment();
        log.info("[NotificationBatch] skip userId={}, petId={}, reason=no recommendation result",
                target.getUserId(),
                pet.getPetId());
        return;
      }

      // 카카오 전송용 메시지 생성
      String message = notificationMessageBuilder.buildMessage(
              target,
              pet,
              topPlace,
              recommendationResult.getMessage(),
              recommendationResult.getWeatherType(),
              recommendationResult.getWeatherWalkLevel()
      );

      // 알림 전송
      NotificationResponse notificationResponse = kakaoNotificationService.send(target, topPlace, message);
      // 전송 성공 시 발송 시각 갱신 및 daily cache 저장
      if (notificationResponse.isSuccess()) {
        markNotificationSent(target);
        userRepository.save(target);
        dailyRecommendationCacheService.saveToday(target.getUserId(), recommendationResult, batchExecutionId);

        successCount.increment();
        log.info("[NotificationBatch] success userId={}, petId={}, placeId={}, code={}",
                target.getUserId(),
                pet.getPetId(),
                topPlace.getId(),
                notificationResponse.getCode());
        return;
      }

      failureCount.increment();
      log.warn("[NotificationBatch] notification failed userId={}, petId={}, placeId={}, code={}, message={}",
              target.getUserId(),
              pet.getPetId(),
              topPlace.getId(),
              notificationResponse.getCode(),
              notificationResponse.getMessage());
    } catch (Exception e) {
      failureCount.increment();
      log.error("[NotificationBatch] target error userId={}, error={}",
              target.getUserId(),
              e.getMessage(),
              e);
    }
  }

  /**
   * User 엔티티의 lastNotificationSentAt 값을 현재 시각으로 갱신한다.
   *
   * <p>setter 노출 없이 배치 내부에서만 제한적으로 상태를 변경하기 위해
   * reflection 기반으로 필드를 업데이트한다.</p>
   */
  private void markNotificationSent(User target) {
    Field field = ReflectionUtils.findField(User.class, "lastNotificationSentAt");
    if (field == null) {
      throw new IllegalStateException("lastNotificationSentAt field not found");
    }
    ReflectionUtils.makeAccessible(field);
    ReflectionUtils.setField(field, target, LocalDateTime.now(SEOUL_ZONE));
  }
}
