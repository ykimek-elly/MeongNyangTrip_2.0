package com.team.meongnyang.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRequest {
    private Long postId;
    private List<String> friendIds;
    private String message;
}
