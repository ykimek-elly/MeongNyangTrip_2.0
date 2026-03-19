package com.team.meongnyang.review.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.review.dto.ReviewDto;
import com.team.meongnyang.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성
     * POST /api/v1/reviews/{placeId}
     */
    @PostMapping("/{placeId}")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long placeId,
            @RequestBody ReviewDto.Request request) {

        ReviewDto.Response response = reviewService.createReview(
                userDetails.getUsername(), placeId, request);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 등록됐어요!", response));
    }

    /**
     * 장소 리뷰 목록 조회
     * GET /api/v1/reviews/{placeId}
     */
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<ReviewDto.PlaceReviewsResponse>> getReviewsByPlace(
            @PathVariable Long placeId) {

        ReviewDto.PlaceReviewsResponse response = reviewService.getReviewsByPlace(placeId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    /**
     * 내 리뷰 목록 조회
     * GET /api/v1/reviews/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReviewDto.Response>>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ReviewDto.Response> response = reviewService.getMyReviews(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    /**
     * 리뷰 삭제
     * DELETE /api/v1/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {

        reviewService.deleteReview(userDetails.getUsername(), reviewId);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제됐어요.", null));
    }
}
