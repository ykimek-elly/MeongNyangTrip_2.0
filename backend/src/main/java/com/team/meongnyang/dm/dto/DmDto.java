package com.team.meongnyang.dm.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

public class DmDto {

    @Getter
    @Builder
    public static class ConversationResponse {
        private String partnerId; // PK of the partner
        private String partnerImg;
        private String partnerNickname; // Added for display
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private long unreadCount;
    }

    @Getter
    @Builder
    public static class MessageResponse {
        private Long id;
        private String fromId; // PK of the sender
        private String content;
        private LocalDateTime createdAt;
        private boolean isRead;
    }

    @Getter
    public static class SendRequest {
        private String content;
    }
}
