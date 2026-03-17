package com.team.meongnyang.batch.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기 알림 배치 스케줄러.
 *
 * 알림 발송 대상 사용자를 주기적으로 조회하고,
 * 추천 생성 및 알림 발송 흐름을 시작하는 배치 진입점 역할을 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  private final NotificationBatchService notificationBatchService;

  /**
   * 매일 오전 9시에 알림 발송 배치를 실행한다.
   */
  @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
  public void runDailyNotificationBatch() {
    log.info("[배치 시작] 일일 추천 알림 배치 실행");
    notificationBatchService.process();
    log.info("[배치 종료] 일일 추천 알림 배치 종료");
  }


  /**
   * 개발 중 수동 확인용 임시 스케줄.
   * 필요 시 잠깐 열어서 로그 동작 여부만 확인하고, 확인 후 주석 처리한다.
   */
  public void runTest () {
    log.info("[배치 시작] 테스트 알림 발송 배치 실행");
    notificationBatchService.process();
    log.info("[배치 종료] 테스트 알림 발송 배치 종료");
  }
}
