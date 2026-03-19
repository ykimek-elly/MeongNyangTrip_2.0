package com.team.meongnyang.user.service;

import com.team.meongnyang.user.dto.ChangePasswordRequest;
import com.team.meongnyang.user.dto.UpdateProfileRequest;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정보 수정 서비스 (프로필·비밀번호·계정 삭제).
 * 현재 인증된 사용자의 이메일을 기준으로 조회한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 닉네임 수정 */
    @Transactional
    public void updateProfile(String email, UpdateProfileRequest request) {
        User user = getByEmail(email);

        String newNickname = request.getNickname().trim();
        if (!user.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        user.updateNickname(newNickname);
    }

    /** 비밀번호 변경 (소셜 로그인 사용자는 password가 null — 변경 불가) */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getByEmail(email);

        if (user.getPassword() == null) {
            throw new RuntimeException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 올바르지 않습니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    /** 회원 탈퇴 — DELETED 소프트 딜리트 */
    @Transactional
    public void deleteAccount(String email) {
        User user = getByEmail(email);
        user.markAsDeleted();
    }

    private User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}
