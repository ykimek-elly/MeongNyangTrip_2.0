package com.team.meongnyang.recommendation.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 응답 DTO.
 * 외부 알림 API 호출 결과를 내부에서 공통적으로 다루기 위한 응답 객체.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

  /** 발송 성공 여부 */
  private boolean success;

  /** 응답 코드 */
  private String code;

  /** 응답 메시지 */
  private String message;
}
