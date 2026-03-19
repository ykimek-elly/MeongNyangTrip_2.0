package com.team.meongnyang.batch.notification;

import com.team.meongnyang.place.entity.Place;
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
import org.springframework.stereotype.Service;

import java.util.List;

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
    log.info("[배치 시작] 추천 알림 배치를 시작합니다.");
    long startTime = System.currentTimeMillis();

    List<User> targets = getNotificationTargets();
    log.info("[배치] 알림 대상 사용자 수={}", targets.size());

    int successCount = 0;
    int failureCount = 0;
    int skipCount = 0;

    for (User target : targets) {
      try {
        log.info("[사용자 처리 시작] userId={}, email={}", target.getUserId(), target.getEmail());

        Pet pet = petRepository.findByUserUserIdAndIsRepresentativeTrue(target.getUserId()).orElse(null);
        if (pet == null) {
          skipCount++;
          log.info("[사용자 스킵] userId={}, email={}, 사유=반려동물 정보 없음",
                  target.getUserId(),
                  target.getEmail());
          continue;
        }

        /** 추천 결과를 생성하는 메서드 호출 */
        RecommendationNotificationResult recommendationResult =
                recommendationPipelineService.recommendForNotification(target, pet);

        Place topPlace = recommendationResult.getPlace();
        if (topPlace == null) {
          skipCount++;
          log.info("[사용자 스킵] userId={}, email={}, 사유=추천 결과 없음",
                  target.getUserId(),
                  target.getEmail());
          continue;
        }

        log.info("[추천 top1 추출] userId={}, email={}, placeId={}, contentId={}, placeTitle={}",
                target.getUserId(),
                target.getEmail(),
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


        log.info("[알림 메시지 템플릿 적용] {} ", message);


        // 알림 발송
        NotificationResponse notificationResponse =
                kakaoNotificationService.send(target, topPlace, message);

        if (notificationResponse.isSuccess()) {
          successCount++;
          log.info("[알림 발송 성공] userId={}, email={}, placeTitle={}, code={}, message={}",
                  target.getUserId(),
                  target.getEmail(),
                  topPlace.getTitle(),
                  notificationResponse.getCode(),
                  notificationResponse.getMessage());
          continue;
        }

        failureCount++;
        log.warn("[알림 발송 실패] userId={}, email={}, placeTitle={}, code={}, message={}",
                target.getUserId(),
                target.getEmail(),
                topPlace.getTitle(),
                notificationResponse.getCode(),
                notificationResponse.getMessage());
      } catch (Exception e) {
        failureCount++;
        log.error("[사용자 처리 예외] userId={}, email={}, error={}",
                target.getUserId(),
                target.getEmail(),
                e.getMessage(),
                e);
      }
    }
    long endTime = System.currentTimeMillis();
    log.info("[배치 집계] total={}, success={}, failure={}, skip={}",
            targets.size(),
            successCount,
            failureCount,
            skipCount);
    log.info("[배치 종료] 추천 알림 배치를 종료합니다. 소요 시간: {}ms , 1명 평균 소요시간: {}", endTime - startTime , (endTime - startTime) / targets.size());

  }

  public void process() {
    runDailyNotificationBatch();
  }

  public List<User> getNotificationTargets() {
    List<User> targets = userRepository.findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE);
    log.info("[대상 사용자 조회] 알림 수신 동의 활성 사용자 수={}", targets.size());
    return targets;
  }

}
