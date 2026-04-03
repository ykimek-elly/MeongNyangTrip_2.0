package com.team.meongnyang.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;           // Access Token
    private String refreshToken;    // Refresh Token (신규)
    private Long userId;
    private String email;
    private String nickname;
    private String profileImage;
    private String role;            // "USER" | "ADMIN"
    private String region;
    private Integer activityRadius;
    private String phoneNumber;     // 신규 추가
    private boolean notificationEnabled; // 신규 추가
}
