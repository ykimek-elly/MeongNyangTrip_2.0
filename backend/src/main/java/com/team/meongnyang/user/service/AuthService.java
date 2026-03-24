package com.team.meongnyang.user.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.security.JwtUtil;
import com.team.meongnyang.user.dto.AuthResponse;
import com.team.meongnyang.user.dto.LoginRequest;
import com.team.meongnyang.user.dto.SignupRequest;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TEMP_PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail());
        return new AuthResponse(token, saved.getUserId(), saved.getEmail(), saved.getNickname(), saved.getProfileImage(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getUserId(), user.getEmail(), user.getNickname(), user.getProfileImage(), user.getRole().name());
    }

    /**
     * 아이디(이메일) 찾기 — 닉네임 + 전화번호로 조회 후 마스킹된 이메일 반환.
     * 가입한 이메일로 결과 발송.
     */
    public String findId(String nickname, String phoneNumber) {
        User user = userRepository.findByNicknameAndPhoneNumber(nickname, phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다."));
        String masked = maskEmail(user.getEmail());
        emailService.sendFindIdEmail(user.getEmail(), masked);
        return masked;
    }

    /**
     * 임시 비밀번호 발급 — 이메일 + 전화번호로 인증 후 8자리 임시 비밀번호 발송 및 DB 업데이트.
     */
    @Transactional
    public void resetPassword(String email, String phoneNumber) {
        User user = userRepository.findByEmailAndPhoneNumber(email, phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다."));
        if (user.getProvider() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다.");
        }
        String tempPassword = generateTempPassword(8);
        user.updatePassword(passwordEncoder.encode(tempPassword));
        emailService.sendTempPasswordEmail(email, tempPassword);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return email.charAt(0) + "***" + email.substring(at);
        return email.substring(0, 2) + "*".repeat(at - 2) + email.substring(at);
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TEMP_PW_CHARS.charAt(RANDOM.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
    }
}