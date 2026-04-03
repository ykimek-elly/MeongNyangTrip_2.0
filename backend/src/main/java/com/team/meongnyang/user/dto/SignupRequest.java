package com.team.meongnyang.user.dto;

import lombok.Getter;

@Getter
public class SignupRequest {
    private String email;
    private String password;
    private String nickname;
    private String phoneNumber;
    private Boolean notificationEnabled;
}