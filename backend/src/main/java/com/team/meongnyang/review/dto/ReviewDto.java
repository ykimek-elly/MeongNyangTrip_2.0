package com.team.meongnyang.review.dto;

import com.team.meongnyang.review.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewDto {

    /** 리뷰 작성 요청 */
    @Getter
    public static class Request {
        private String content;
        private Double rating;
        private String imageUrl; // 선택
    }

    /** 리뷰 단건 응답 */
    @Getter
    public static class Response {
        private final Long reviewId;
        private final Long userId;
        private final String nickname;
        private final String profileImage;
        private final String content;
        private final Double rating;
        private final String imageUrl;
        private final LocalDateTime createdAt;

        public Response(Review r) {
            this.reviewId     = r.getReviewId();
            this.userId       = r.getUser().getUserId();
            this.nickname     = r.getUser().getNickname();
            this.profileImage = r.getUser().getProfileImage();
            this.content      = r.getContent();
            this.rating       = r.getRating();
            this.imageUrl     = r.getImageUrl();
            this.createdAt    = r.getRegDate();
        }
    }

    /** 장소 리뷰 목록 응답 (평균 포함) */
    @Getter
    public static class PlaceReviewsResponse {
        private final double averageRating;
        private final long totalCount;
        private final List<Response> reviews;

        public PlaceReviewsResponse(double averageRating, long totalCount, List<Response> reviews) {
            this.averageRating = averageRating;
            this.totalCount    = totalCount;
            this.reviews       = reviews;
        }
    }
}
