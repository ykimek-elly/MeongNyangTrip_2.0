package com.team.meongnyang.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
        @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
        @Size(max = 1000, message = "메시지는 최대 1000자까지 입력 가능합니다.")
        private String content;
    }
}
