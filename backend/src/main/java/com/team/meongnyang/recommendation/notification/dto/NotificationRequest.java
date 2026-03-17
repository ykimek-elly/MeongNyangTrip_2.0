package com.team.meongnyang.recommendation.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 요청 DTO.
 * 현재는 카카오 알림 발송을 기준으로 사용하고,
 * 이후 이메일/푸시 등 다른 채널로 확장 가능하도록 공통 형태로 둔다.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
  /** 수신자 전화번호 */
  private String phoneNumber;

  /** 카카오 템플릿 코드 */
  private String templateCode;

  /** 발신 프로필 키 */
  private String senderKey;

  /** 메시지 제목 */
  private String title;

  /** 메시지 본문 */
  private String message;
}
