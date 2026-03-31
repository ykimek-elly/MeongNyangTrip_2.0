package com.team.meongnyang.user.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.security.JwtUtil;
import com.team.meongnyang.security.RefreshTokenService;
import com.team.meongnyang.user.dto.AuthResponse;
import com.team.meongnyang.user.dto.LoginRequest;
import com.team.meongnyang.user.dto.SignupRequest;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
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
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Access Token 재발급.
     * Body: { "refreshToken": "..." }
     * 성공 시 새 accessToken + refreshToken 반환 (Refresh Token Rotation)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || !refreshTokenService.validate(refreshToken)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "유효하지 않은 리프레시 토큰입니다."));
        }

        String email = refreshTokenService.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Refresh Token Rotation: 기존 토큰 삭제 후 새로 발급
        String newAccessToken  = jwtUtil.generateToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);
        refreshTokenService.delete(email);
        refreshTokenService.save(email, newRefreshToken);

        return ResponseEntity.ok(new AuthResponse(
                newAccessToken, newRefreshToken,
                user.getUserId(), user.getEmail(),
                user.getNickname(), user.getProfileImage(),
                user.getRole().name()
        ));
    }

    /** 아이디 찾기 */
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<Map<String, String>>> findId(@RequestBody Map<String, String> body) {
        String masked = authService.findId(body.get("name"), body.get("phone"));
        return ResponseEntity.ok(ApiResponse.success("SUCCESS", Map.of("email", masked)));
    }

    /** 비밀번호 재설정 */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("email"), body.get("phone"));
        return ResponseEntity.ok(ApiResponse.success("임시 비밀번호가 이메일로 발송되었습니다.", null));
    }
}
