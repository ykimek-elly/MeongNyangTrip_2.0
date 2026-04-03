package com.team.meongnyang.user.controller;

import com.team.meongnyang.user.dto.ChangePasswordRequest;
import com.team.meongnyang.user.dto.UpdateProfileRequest;
import com.team.meongnyang.user.service.UserService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 정보 수정 컨트롤러 (인증 필요).
 *
 * PUT  /api/v1/users/profile   — 닉네임 수정
 * PUT  /api/v1/users/password  — 비밀번호 변경
 * DELETE /api/v1/users/me      — 회원 탈퇴 (소프트 딜리트)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/v1/users/location — 활동 지역 좌표 저장.
     * 온보딩 완료 시 호출. latitude/longitude 미전달 시 서울 강남구 기본값 적용.
     */
    @PatchMapping("/location")
    public ResponseEntity<Void> saveLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LocationRequest request) {
        userService.saveLocation(userDetails.getUsername(), request.getLatitude(), request.getLongitude(), request.getActivityRadius(), request.getRegion());
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/v1/users/phone — 휴대폰 번호 업데이트.
     */
    @PatchMapping("/phone")
    public ResponseEntity<Void> updatePhone(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PhoneRequest request) {
        userService.updatePhoneNumber(userDetails.getUsername(), request.getPhone());
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/v1/users/notifications — 알림 설정 업데이트.
     */
    @PatchMapping("/notifications")
    public ResponseEntity<Void> updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody NotificationRequest request) {
        userService.updateNotificationEnabled(userDetails.getUsername(), request.isNotificationEnabled());
        return ResponseEntity.ok().build();
    }

    @Getter
    @NoArgsConstructor
    static class LocationRequest {
        private Double latitude;
        private Double longitude;
        private Integer activityRadius;
        private String region;
    }

    @Getter
    @NoArgsConstructor
    static class PhoneRequest {
        private String phone;
    }

    @Getter
    @NoArgsConstructor
    static class NotificationRequest {
        private boolean notificationEnabled;
    }
}
