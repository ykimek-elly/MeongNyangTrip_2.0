package com.team.meongnyang.recommendation.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 알림 발송 요청 DTO.
 * 현재는 카카오 알림톡 발송을 기준으로 사용하고,
 * 이후 이메일 메시지 등 다른 채널로도 확장 가능하도록 공통 형태로 둔다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationRequest {

  private String templateCode;
  private String plusFriendId;
  private List<Message> messages;

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Message {
    private String to;
    private String content;
    private Map<String, String> templateParameter;
    private List<Button> buttons;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Button {
    private String type;        // WL, AL, AC
    private String name;
    private String linkMobile;
  }
}
