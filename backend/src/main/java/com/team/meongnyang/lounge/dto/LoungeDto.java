package com.team.meongnyang.lounge.dto;

import com.team.meongnyang.lounge.entity.LoungeComment;
import com.team.meongnyang.lounge.entity.LoungePost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LoungeDto {

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        private String content;
        private String imageUrl;
        private Long placeId;
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
        private int likes;
        private boolean isLiked;
        private int comments;
        private String time;
        private LocalDateTime createdAt;
        private List<CommentResponse> commentList;

        public static PostResponse from(LoungePost post, String currentUserEmail) {
            return PostResponse.builder()
                    .id(post.getPostId())
                    .user(post.getUser().getNickname())
                    .userImg(post.getUser().getProfileImage())
                    .content(post.getContent())
                    .img(post.getImageUrl())
                    .placeId(post.getPlaceId())
                    .likes(post.getLikes())
                    .isLiked(post.getLikeList().stream()
                            .anyMatch(l -> l.getUser().getEmail().equals(currentUserEmail)))
                    .comments(post.getCommentList().size())
                    .time("방금 전")
                    .createdAt(null)
                    .commentList(post.getCommentList().stream()
                            .map(CommentResponse::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter @Builder
    public static class CommentResponse {
        private Long id;
        private String user;
        private String content;
        private LocalDateTime createdAt;

        public static CommentResponse from(LoungeComment comment) {
            return CommentResponse.builder()
                    .id(comment.getCommentId())
                    .user(comment.getUser().getNickname())
                    .content(comment.getContent())
                    .createdAt(null)
                    .build();
        }
    }
}