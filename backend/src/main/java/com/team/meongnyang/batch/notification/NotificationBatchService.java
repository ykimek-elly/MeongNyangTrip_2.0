package com.team.meongnyang.batch.notification;

import com.team.meongnyang.recommendation.notification.dto.NotificationProcessResult;
import com.team.meongnyang.recommendation.notification.service.NotificationRecommendationProcessor;
import com.team.meongnyang.recommendation.notification.service.NotificationTargerReader;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 일일 추천 알림 배치 서비스.
 *
 * 스케줄러에서 호출되며,
 * 발송 대상 사용자 조회, 추천 생성, 알림 발송, 결과 기록을 순차적으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {

  private final NotificationTargerReader notificationTargerReader;
  private final NotificationRecommendationProcessor notificationRecommendationProcessor;

  /**
   *       1. 알림 대상 사용자 조회
   *       2. 사용자별 반복
   *       3. 추천 가능 여부 검사
   *       4. 추천 오케스트레이션 호출
   *       5. 알림 발송
   *       6. 발송 로그 저장
   *       7. 발송 시각 업데이트
   */
  public void process() {
    log.info("[배치] 알림 발송 대상 사용자 조회 시작");

    // 1. 알림 수신 동의 사용자 조회
    List<User> targets = notificationTargerReader.getNotificationTargets();
    log.info("[배치] 알림 발송 대상 사용자 조회 완료, {}명", targets.size());
    // 2. 사용자별 추천 생성
    for (User target : targets) {

      try {
        NotificationProcessResult result = notificationRecommendationProcessor.process(target);

        log.info("[알림 배치 사용자 처리 결과] userId={}, email={}, success={}, message={}",
                result.userId(),
                result.email(),
                result.success(),
                result.message());
      } catch (Exception e) {
        log.error("[알림 배치 사용자 처리 실패] userId={}, email={}, error={}",
                target.getUserId(),
                target.getEmail(),
                e.getMessage());
      }

    }
    // 3. 알림 발송
    // 4. 발송 성공/실패 로그 저장
    log.info("[알림 배치 종료] 대상 사용자 수={}", targets.size());
  }


  public void processSingleUser (User user) {
    log.info("[알림 배치 사용자 처리 시작] 사용자 {} 알림 발송 시작", user.getUserId());
  }

}
