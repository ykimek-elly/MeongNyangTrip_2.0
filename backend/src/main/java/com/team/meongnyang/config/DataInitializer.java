package com.team.meongnyang.config;

import com.team.meongnyang.place.service.PlaceService;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlaceService placeService;

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfNotExists("admin@test.com", "password1234", "관리자");
        createAdminIfNotExists("admin@meongtrip.com", "Meong1234!", "관리자2");
        warmUpPlacesCache();
    }

    private void warmUpPlacesCache() {
        try {
            placeService.getPlaces(null, null);
            log.info("[DataInitializer] 장소 목록 캐시 워밍 완료");
        } catch (Exception e) {
            log.warn("[DataInitializer] 장소 목록 캐시 워밍 실패 (무시): {}", e.getMessage());
        }
    }

    private void createAdminIfNotExists(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            log.info("[DataInitializer] 관리자 계정이 이미 존재합니다: {}", email);
            return;
        }
        try {
            User admin = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .nickname(nickname)
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("[DataInitializer] 관리자 계정 생성 완료: {}", email);
        } catch (Exception e) {
            log.warn("[DataInitializer] 관리자 계정 생성 스킵 (이미 존재): {}", e.getMessage());
        }
    }
}
