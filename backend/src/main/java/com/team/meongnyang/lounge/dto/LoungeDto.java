package com.team.meongnyang.lounge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.meongnyang.lounge.entity.LoungeComment;
import com.team.meongnyang.lounge.entity.LoungePost;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class LoungeDto {

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        private String content;
        private String imageUrl;
        private Long placeId;
        private String postType; // "FEED" or "TALK"
    }

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        private String content;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class CommentRequest {
        private String content;
    }

    @Getter @Builder
    public static class PostResponse {
        private Long id;
        private String user;
        private String userImg;
        private String content;
        private String img;
        private Long placeId;
        private String postType;
        private int likes;
        @JsonProperty("isLiked")
        private boolean isLiked;
        @JsonProperty("isOwner")
        private boolean isOwner;
        private int comments;
        private String time;
        private LocalDateTime createdAt;
        private List<CommentResponse> commentList;

        public static PostResponse from(LoungePost post, String currentUserEmail) {
            String email = currentUserEmail != null ? currentUserEmail : "";
            return PostResponse.builder()
                    .id(post.getPostId())
                    .user(post.getUser().getNickname())
                    .userImg(post.getUser().getProfileImage())
                    .content(post.getContent())
                    .img(post.getImageUrl())
                    .placeId(post.getPlaceId())
                    .postType(post.getPostType())
                    .likes(post.getLikes())
                    .isLiked(post.getLikeList().stream()
                            .anyMatch(l -> l.getUser().getEmail().equals(email)))
                    .isOwner(post.getUser().getEmail().equals(email))
                    .comments(post.getCommentList().size())
                    .time(formatRelativeTime(post.getRegDate()))   // ← 수정: 하드코딩 제거
                    .createdAt(post.getRegDate())
                    .commentList(post.getCommentList().stream()
                            .map(c -> CommentResponse.from(c, email))  // ← 수정: email 전달
                            .collect(Collectors.toList()))
                    .build();
        }

        /** 상대 시간 포매팅 (방금 전 / N분 전 / N시간 전 / N일 전) */
        private static String formatRelativeTime(LocalDateTime dateTime) {
            if (dateTime == null) return "";
            long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
            if (minutes < 1)  return "방금 전";
            if (minutes < 60) return minutes + "분 전";
            long hours = minutes / 60;
            if (hours < 24)   return hours + "시간 전";
            long days = hours / 24;
            return days + "일 전";
        }
    }

    @Getter @Builder
    public static class CommentResponse {
        private Long id;
        private String user;
        private String content;
        private LocalDateTime createdAt;
        @JsonProperty("isOwner")          // ← 추가: 댓글 소유자 여부
        private boolean isOwner;

        public static CommentResponse from(LoungeComment comment, String currentUserEmail) {
            String email = currentUserEmail != null ? currentUserEmail : "";
            return CommentResponse.builder()
                    .id(comment.getCommentId())
                    .user(comment.getUser().getNickname())
                    .content(comment.getContent())
                    .createdAt(comment.getRegDate())
                    .isOwner(comment.getUser().getEmail().equals(email))  // ← 추가
                    .build();
        }
    }
}