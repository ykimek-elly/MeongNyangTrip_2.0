package com.team.meongnyang.recommendation.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 응답 DTO.
 * 외부 알림 API 호출 결과를 서비스 계층에서 공통으로 다루기 위한 응답 객체이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
  private boolean success;
  private String requestId;
  private String requestTime;
  private String statusCode;
  private String statusName;

  public String getCode() {
    return statusCode;
  }

  public String getMessage() {
    return statusName;
  }

  public static NotificationResponse failure(String statusCode, String statusName) {
    return NotificationResponse.builder()
            .success(false)
            .statusCode(statusCode)
            .statusName(statusName)
            .build();
  }
}
