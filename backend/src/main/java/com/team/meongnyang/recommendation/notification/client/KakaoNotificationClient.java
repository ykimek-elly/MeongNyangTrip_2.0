package com.team.meongnyang.recommendation.notification.client;

import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 카카오 알림 외부 연동 전용 클라이언트.
 *
 * 현재는 실제 API 연동 전 단계이므로 mock 형태로 동작한다.
 * 이후 RestClient 또는 WebClient를 사용해 실제 카카오 API 호출로 교체한다.
 */

@Component
@Slf4j
public class KakaoNotificationClient {

  // todo : 카카오 알림 API 연동을 위한 클라이언트 구현

  public NotificationResponse send(NotificationRequest request) {
    log.info("[KAKAO MOCK SEND] phone : {}, title : {}, message : {}", request.getPhoneNumber(), request.getTitle(), request.getMessage());
    return NotificationResponse.builder()
        .success(true)
        .code("200")
        .message("Notification sent successfully")
        .build();
  }
}
