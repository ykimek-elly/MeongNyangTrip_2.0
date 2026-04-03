package com.team.meongnyang.user.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
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
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(newNickname);
    }

    /** 비밀번호 변경 (소셜 로그인 사용자는 password가 null — 변경 불가) */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getByEmail(email);

        if (user.getPassword() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    /** 회원 탈퇴 — DELETED 소프트 딜리트 */
    @Transactional
    public void deleteAccount(String email) {
        User user = getByEmail(email);
        user.markAsDeleted();
    }

    /**
     * 활동 지역 좌표 + 반경 저장.
     * 미선택 시 기본값: 서울 강남구 (37.5172, 127.0473), 반경 15km
     */
    @Transactional
    public void saveLocation(String email, Double latitude, Double longitude, Integer activityRadius, String region) {
        User user = getByEmail(email);
        double lat = (latitude != null) ? latitude : 37.5172;
        double lng = (longitude != null) ? longitude : 127.0473;
        int radius = (activityRadius != null) ? activityRadius : 15;
        user.updateLocation(lat, lng, radius, region);
    }

    /** 휴대폰 번호 업데이트 */
    @Transactional
    public void updatePhoneNumber(String email, String phoneNumber) {
        User user = getByEmail(email);
        user.updatePhoneNumber(phoneNumber);
    }

    /** 온보딩 입력을 한 트랜잭션에서 함께 저장한다. */
    @Transactional
    public void saveOnboarding(
            String email,
            String nickname,
            String phoneNumber,
            Double latitude,
            Double longitude,
            Integer activityRadius,
            String region
    ) {
        User user = getByEmail(email);

        if (nickname != null) {
            String newNickname = nickname.trim();
            if (!newNickname.isBlank() && !user.getNickname().equals(newNickname)
                    && userRepository.existsByNickname(newNickname)) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
            if (!newNickname.isBlank()) {
                user.updateNickname(newNickname);
            }
        }

        if (phoneNumber != null) {
            user.updatePhoneNumber(phoneNumber);
        }

        double lat = (latitude != null) ? latitude : 37.5172;
        double lng = (longitude != null) ? longitude : 127.0473;
        int radius = (activityRadius != null) ? activityRadius : 15;
        user.updateLocation(lat, lng, radius, region);
    }

    /** 알림 설정 업데이트 */
    @Transactional
    public void updateNotificationEnabled(String email, boolean enabled) {
        User user = getByEmail(email);
        user.updateNotificationEnabled(enabled);
    }

    private User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
