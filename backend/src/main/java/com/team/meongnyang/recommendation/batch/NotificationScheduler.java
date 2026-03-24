package com.team.meongnyang.recommendation.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기 알림 배치 스케줄러.
 *
 * 알림 발송 대상 사용자를 주기적으로 조회하고,
 * 추천 생성 및 알림 발송 흐름을 시작하는 진입점 역할을 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  private final NotificationBatchService notificationBatchService;
  private final WeatherBatchService weatherBatchService;
  /**
   * 매일 오전 9시에 추천 알림 배치를 실행한다.
   */
  @Scheduled(cron = "${batch.notification-cron}", zone = "Asia/Seoul")
  public void runDailyNotificationBatch() {
    log.info("[스케줄러 시작] 일일 추천 알림 배치 실행");
    notificationBatchService.runDailyNotificationBatch();
    log.info("[스케줄러 종료] 일일 추천 알림 배치 종료");
  }

  /**
   * 매일 runDailyNotificationBatch 실행 10분 전 날씨 정보 미리 로드를 실행한다.
   */
  @Scheduled(cron = "${batch.weather-preload-cron}", zone = "Asia/Seoul")
  public void runWeatherPreloadBatch() {
    log.info("[스케줄러 시작] 날씨 정보 미리 로드");
    weatherBatchService.runWeatherPreloadBatch();
    log.info("[스케줄러 종료] 날씨 정보 미리 로드");
  }

}
