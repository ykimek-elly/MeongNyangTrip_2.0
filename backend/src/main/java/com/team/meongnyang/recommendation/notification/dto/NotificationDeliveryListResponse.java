package com.team.meongnyang.recommendation.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDeliveryListResponse {

    private String requestId;
    private String statusCode;
    private String statusName;
    private Integer pageSize;
    private Integer pageIndex;
    private Integer itemCount;
    private Boolean hasMore;
    private String nextToken;
    private List<DeliveryMessage> messages;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliveryMessage {
        private String requestTime;
        private String requestId;
        private String messageId;
        private String countryCode;
        private String to;
        private String content;
        private String plusFriendId;
        private String templateCode;
        private String completeTime;
        private String requestStatusCode;
        private String requestStatusName;
        private String requestStatusDesc;
        private String messageStatusCode;
        private String messageStatusName;
        private String messageStatusDesc;
    }
}
