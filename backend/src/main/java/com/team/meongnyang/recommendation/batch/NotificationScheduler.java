package com.team.meongnyang.recommendation.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기 알림 배치 스케줄러다.
 *
 * <p>스케줄은 여러 번 실행될 수 있고, 실제 발송 가능 여부는 정책 서비스가 최종 판단한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationBatchService notificationBatchService;
    private final WeatherBatchService weatherBatchService;

    /**
     * 추천 알림 배치를 실행한다.
     */
    @Scheduled(cron = "${batch.notification-cron}", zone = "Asia/Seoul")
    public void runDailyNotificationBatch() {
        log.info("[NotificationScheduler] 추천 알림 배치를 시작합니다.");
        notificationBatchService.runDailyNotificationBatch();
        log.info("[NotificationScheduler] 추천 알림 배치를 종료합니다.");
    }

    /**
     * 날씨 캐시 선적재 배치를 실행한다.
     */
    @Scheduled(cron = "${batch.weather-preload-cron}", zone = "Asia/Seoul")
    public void runWeatherPreloadBatch() {
        log.info("[NotificationScheduler] 날씨 선적재 배치를 시작합니다.");
        weatherBatchService.runWeatherPreloadBatch();
        log.info("[NotificationScheduler] 날씨 선적재 배치를 종료합니다.");
    }
}
