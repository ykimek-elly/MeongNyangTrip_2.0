package com.team.meongnyang.recommendation.batch;

import com.team.meongnyang.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch/recommendation-notification")
public class NotificationBatchController {

  private final NotificationBatchService notificationBatchService;
  private final WeatherBatchService weatherBatchService;

  /**
   * 알림 스케줄러 테스트를 위한 API
   * 역할 : 수동으로 스케줄러를 실행하여 알림 배치를 실행합니다.
   * @return 200 OK
   */
  @GetMapping("/test")
  public ApiResponse<String> runRecommendationNotificationBatch() {
    log.info("[배치 API] 추천 알림 배치 수동 실행 요청");
    notificationBatchService.runDailyNotificationBatch();
    return ApiResponse.success(
            "추천 알림 배치 실행 완료",
            "/api/v1/admin/batch/recommendation-batch/run"
    );
  }

  /**
   * 날씨 스케줄러 테스트를 위한 API
   * 역할 : 날씨 정보 미리 로드 배치를 수동으로 실행합니다.
   * @return 200 OK
   */
  @GetMapping("/weather")
  public ApiResponse<String> runPreloadWeatherBatch() {
    log.info("[배치 API] 날씨 정보 미리 로드 배치 수동 실행 요청");
    weatherBatchService.runWeatherPreloadBatch();
    return ApiResponse.success(
            "추천 알림 배치 실행 완료",
            "/api/v1/admin/batch/recommendation-batch/run"
    );
  }
}
