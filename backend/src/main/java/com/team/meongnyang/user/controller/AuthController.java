package com.team.meongnyang.user.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.user.dto.AuthResponse;
import com.team.meongnyang.user.dto.LoginRequest;
import com.team.meongnyang.user.dto.SignupRequest;
import com.team.meongnyang.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** 아이디 찾기 — 닉네임 + 전화번호 → 마스킹된 이메일 반환 + 메일 발송 */
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<Map<String, String>>> findId(@RequestBody Map<String, String> body) {
        String masked = authService.findId(body.get("name"), body.get("phone"));
        return ResponseEntity.ok(ApiResponse.success("SUCCESS", Map.of("email", masked)));
    }

    /** 비밀번호 재설정 — 이메일 + 전화번호 인증 후 임시 비밀번호 메일 발송 */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("email"), body.get("phone"));
        return ResponseEntity.ok(ApiResponse.success("임시 비밀번호가 이메일로 발송되었습니다.", null));
    }
}