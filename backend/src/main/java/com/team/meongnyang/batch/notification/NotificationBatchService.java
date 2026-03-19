package com.team.meongnyang.batch.notification;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.notification.service.KakaoNotificationService;
import com.team.meongnyang.recommendation.notification.service.NotificationMessageBuilder;
import com.team.meongnyang.recommendation.log.RecommendationBatchTraceContext;
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
 * 일일 추천 알림 배치 서비스.
 *
 * 스케줄러나 수동 실행 엔드포인트에서 호출되며,
 * 알림 수신 동의 사용자 조회, 추천 생성, 알림 발송, 결과 로그 기록을 순차적으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {

  private final UserRepository userRepository;
  private final PetRepository petRepository;
  private final RecommendationPipelineService recommendationPipelineService;
  private final KakaoNotificationService kakaoNotificationService;
  private final NotificationMessageBuilder notificationMessageBuilder;
  @Value("${batch.notification-parallelism:4}")
  private int notificationParallelism;
  /**
   *       1. 알림 대상 사용자 조회
   *       2. 사용자별 반복
   *       3. 추천 결과 여부 확인
   *       4. 추천 결과 top1 추출
   *       5. 알림 발송
   *       6. 발송 로그 기록
   *       7. 최종 집계 로그 출력
   */
  public void runDailyNotificationBatch() {
    String batchExecutionId = UUID.randomUUID().toString();
    log.info("[배치 시작] 추천 알림 배치를 시작합니다. batchExecutionId={}", batchExecutionId);
    long startTime = System.currentTimeMillis();

    List<User> targets = getNotificationTargets();
    Map<Long, Pet> representativePetMap = loadRepresentativePets(targets);
    log.info("[배치] 알림 대상 사용자 수={}, 대표 반려동물 수={}, 병렬 처리 수={}",
            targets.size(),
            representativePetMap.size(),
            Math.max(1, Math.min(notificationParallelism, Math.max(targets.size(), 1))));

    LongAdder successCount = new LongAdder();
    LongAdder failureCount = new LongAdder();
    LongAdder skipCount = new LongAdder();

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
                      successCount,
                      failureCount,
                      skipCount
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
            failureCount.increment();
            log.error("[배치 사용자 처리 예외] batchExecutionId={}, error={}", batchExecutionId, e.getMessage(), e);
          }
        }
      }
    }
    long endTime = System.currentTimeMillis();
    log.info("[배치 집계] total={}, success={}, failure={}, skip={}",
            targets.size(),
            successCount.sum(),
            failureCount.sum(),
            skipCount.sum());
    log.info("[배치 종료] 추천 알림 배치를 종료합니다. batchExecutionId={}, 소요 시간: {}ms , 1명 평균 소요시간: {}",
            batchExecutionId,
            endTime - startTime,
            targets.isEmpty() ? 0 : (endTime - startTime) / targets.size());

  }

  public void process() {
    runDailyNotificationBatch();
  }

  public List<User> getNotificationTargets() {
    List<User> targets = userRepository.findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE);
    log.info("[대상 사용자 조회] 알림 수신 동의 활성 사용자 수={}", targets.size());
    return targets;
  }

  /**
   * 사용자별 대표 반려동물을 한 번에 조회해서 배치 실행 중 반복 조회를 줄인다.
   */
  private Map<Long, Pet> loadRepresentativePets(List<User> targets) {
    if (targets.isEmpty()) {
      return Map.of();
    }

    List<Long> userIds = targets.stream()
            .map(User::getUserId)
            .collect(Collectors.toList());

    return petRepository.findAllByUserUserIdInAndIsRepresentativeTrue(userIds).stream()
            .collect(Collectors.toMap(pet -> pet.getUser().getUserId(), pet -> pet, (left, right) -> left, HashMap::new));
  }

  /**
   * 사용자 단위 실행은 독립적으로 처리해서 전체 배치가 한 사용자 오류로 중단되지 않도록 한다.
   */
  private void processTarget(
          String batchExecutionId,
          User target,
          Pet pet,
          LongAdder successCount,
          LongAdder failureCount,
          LongAdder skipCount
  ) {
    try (RecommendationBatchTraceContext.TraceScope ignored =
                 RecommendationBatchTraceContext.open(batchExecutionId, target.getUserId(), pet == null ? null : pet.getPetId())) {
      log.info("[사용자 처리 시작] batchExecutionId={}, userId={}, email={}", batchExecutionId, target.getUserId(), target.getEmail());

      if (pet == null) {
        skipCount.increment();
        log.info("[사용자 스킵] batchExecutionId={}, userId={}, email={}, 사유=대표 반려동물 정보 없음",
                batchExecutionId,
                target.getUserId(),
                target.getEmail());
        return;
      }

      /** 추천 결과를 생성하는 메서드 호출 */
      RecommendationNotificationResult recommendationResult =
              recommendationPipelineService.recommendForNotification(target, pet, batchExecutionId);

      Place topPlace = recommendationResult.getPlace();
      if (topPlace == null) {
        skipCount.increment();
        log.info("[사용자 스킵] batchExecutionId={}, userId={}, email={}, petId={}, 사유=추천 결과 없음",
                batchExecutionId,
                target.getUserId(),
                target.getEmail(),
                pet.getPetId());
        return;
      }

      log.info("[추천 top1 추출] batchExecutionId={}, userId={}, email={}, petId={}, placeId={}, contentId={}, placeTitle={}",
              batchExecutionId,
              target.getUserId(),
              target.getEmail(),
              pet.getPetId(),
              topPlace.getId(),
              topPlace.getContentId(),
              topPlace.getTitle());

      // 알림 메세지 템플릿 적용
      String message = notificationMessageBuilder.buildMessage(
              target,
              pet,
              topPlace,
              recommendationResult.getMessage(),
              recommendationResult.getWeatherType(),
              recommendationResult.getWeatherWalkLevel());

      log.info("[알림 메시지 템플릿 적용] batchExecutionId={}, userId={}, petId={}, message={}",
              batchExecutionId,
              target.getUserId(),
              pet.getPetId(),
              message);

      // 알림 발송
      NotificationResponse notificationResponse =
              kakaoNotificationService.send(target, topPlace, message);

      if (notificationResponse.isSuccess()) {
        successCount.increment();
        log.info("[알림 발송 성공] batchExecutionId={}, userId={}, email={}, petId={}, placeTitle={}, code={}, message={}",
                batchExecutionId,
                target.getUserId(),
                target.getEmail(),
                pet.getPetId(),
                topPlace.getTitle(),
                notificationResponse.getCode(),
                notificationResponse.getMessage());
        return;
      }

      failureCount.increment();
      log.warn("[알림 발송 실패] batchExecutionId={}, userId={}, email={}, petId={}, placeTitle={}, code={}, message={}",
              batchExecutionId,
              target.getUserId(),
              target.getEmail(),
              pet.getPetId(),
              topPlace.getTitle(),
              notificationResponse.getCode(),
              notificationResponse.getMessage());
    } catch (Exception e) {
      failureCount.increment();
      log.error("[사용자 처리 예외] batchExecutionId={}, userId={}, email={}, error={}",
              batchExecutionId,
              target.getUserId(),
              target.getEmail(),
              e.getMessage(),
              e);
    }
  }

}
