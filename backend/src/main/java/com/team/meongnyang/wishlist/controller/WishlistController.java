package com.team.meongnyang.wishlist.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.wishlist.dto.WishlistDto;
import com.team.meongnyang.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 찜하기 토글
     * POST /api/v1/wishlists/{placeId}
     */
    @PostMapping("/{placeId}")
    public ResponseEntity<ApiResponse<WishlistDto.ToggleResponse>> toggle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long placeId) {

        WishlistDto.ToggleResponse response = wishlistService.toggle(
                userDetails.getUsername(), placeId);

        String msg = response.isWishlisted() ? "찜 목록에 추가됐어요!" : "찜 목록에서 제거됐어요.";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    /**
     * 내 찜 목록 조회
     * GET /api/v1/wishlists/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WishlistDto.Response>>> getMyWishlists(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<WishlistDto.Response> response = wishlistService.getMyWishlists(
                userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("찜 목록 조회 성공", response));
    }

    /**
     * 특정 장소 찜 여부 확인
     * GET /api/v1/wishlists/{placeId}/status
     */
    @GetMapping("/{placeId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isWishlisted(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long placeId) {

        boolean wishlisted = wishlistService.isWishlisted(userDetails.getUsername(), placeId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", wishlisted));
    }
}
