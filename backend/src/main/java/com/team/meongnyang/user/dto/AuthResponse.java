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
}
