package com.team.meongnyang.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDto {
    private String id; // 상대방 userId
    private String name; // 닉네임
    private String profileImg;
    private String petName;
    private String petType;
    private boolean isVerified;
}
