package com.team.meongnyang.batch.notification;

import com.team.meongnyang.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch/recommendation-notification")
public class NotificationBatchController {

  private final NotificationBatchService notificationBatchService;

  @GetMapping("/batch/noti/run")
  public ApiResponse<String> runRecommendationNotificationBatch() {
    log.info("[배치 API] 추천 알림 배치 수동 실행 요청");
    notificationBatchService.runDailyNotificationBatch();
    return ApiResponse.success(
            "추천 알림 배치 실행 완료",
            "/api/v1/admin/batch/recommendation-notification/run"
    );
  }
}
