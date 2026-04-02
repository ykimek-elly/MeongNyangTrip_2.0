package com.team.meongnyang.recommendation.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDeliveryResult {

    private boolean success;
    private String requestId;
    private String messageId;
    private String requestTime;
    private String completeTime;
    private String plusFriendId;
    private String templateCode;
    private String to;
    private String content;
    private String requestStatusCode;
    private String requestStatusName;
    private String requestStatusDesc;
    private String messageStatusCode;
    private String messageStatusName;
    private String messageStatusDesc;

    public boolean hasRequestStatus() {
        return StringUtils.hasText(requestStatusCode);
    }

    public boolean hasFinalMessageStatus() {
        return StringUtils.hasText(messageStatusCode);
    }

    public boolean isAccepted() {
        return "A000".equalsIgnoreCase(requestStatusCode);
    }

    public static NotificationDeliveryResult failure(String requestStatusCode, String requestStatusDesc) {
        return NotificationDeliveryResult.builder()
                .success(false)
                .requestStatusCode(requestStatusCode)
                .requestStatusDesc(requestStatusDesc)
                .build();
    }
}
