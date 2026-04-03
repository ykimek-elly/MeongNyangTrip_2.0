package com.team.meongnyang.recommendation.batch;

import com.team.meongnyang.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 알림 배치 및 날씨 배치 스케줄러를 수동으로 실행하기 위한 관리자용 컨트롤러
 *
 * 실제 스케줄러 실행 전 테스트 및 운영 확인을 위해
 * 배치 작업을 API 형태로 직접 실행할 수 있도록 제공한다.
 */
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
    log.info("[추천 배치] 수동 실행 요청");
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
    log.info("[날씨 조회] 선적재 배치 수동 실행 요청");
    weatherBatchService.runWeatherPreloadBatch();
    return ApiResponse.success(
            "추천 알림 배치 실행 완료",
            "/api/v1/admin/batch/recommendation-batch/run"
    );
  }
}
