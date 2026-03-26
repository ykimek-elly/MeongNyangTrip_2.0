package com.team.meongnyang.lounge.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.lounge.dto.LoungeDto;
import com.team.meongnyang.lounge.service.LoungeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lounge")
@RequiredArgsConstructor
public class LoungeController {

    private final LoungeService loungeService;

    /** 피드/톡 조회 (비로그인도 가능) ?type=FEED or TALK */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<LoungeDto.PostResponse>>> getPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "FEED") String type) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.success("조회 성공", loungeService.getPosts(email, type)));
    }

    /** 게시글 작성 */
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<LoungeDto.PostResponse>> createPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LoungeDto.CreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("게시글이 등록됐어요!",
                loungeService.createPost(userDetails.getUsername(), req)));
    }

    /** 게시글 수정 */
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<LoungeDto.PostResponse>> updatePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @RequestBody LoungeDto.UpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("수정됐어요!",
                loungeService.updatePost(userDetails.getUsername(), postId, req)));
    }

    /** 게시글 삭제 */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        loungeService.deletePost(userDetails.getUsername(), postId);
        return ResponseEntity.ok(ApiResponse.success("삭제됐어요.", null));
    }

    /** 좋아요 토글 */
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<LoungeDto.PostResponse>> toggleLike(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("좋아요!",
                loungeService.toggleLike(userDetails.getUsername(), postId)));
    }

    /** 댓글 작성 */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<LoungeDto.CommentResponse>> addComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @RequestBody LoungeDto.CommentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("댓글이 달렸어요!",
                loungeService.addComment(userDetails.getUsername(), postId, req)));
    }

    /** 댓글 삭제 */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        loungeService.deleteComment(userDetails.getUsername(), postId, commentId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제됐어요.", null));
    }
}