package com.team.meongnyang.config;

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

    private static final String ADMIN_EMAIL    = "admin@test.com";
    private static final String ADMIN_PASSWORD = "password1234";
    private static final String ADMIN_NICKNAME = "관리자";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("[DataInitializer] 관리자 계정이 이미 존재합니다: {}", ADMIN_EMAIL);
            return;
        }
        try {
            User admin = User.builder()
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .nickname(ADMIN_NICKNAME)
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("[DataInitializer] 관리자 계정 생성 완료: {}", ADMIN_EMAIL);
        } catch (Exception e) {
            log.warn("[DataInitializer] 관리자 계정 생성 스킵 (이미 존재): {}", e.getMessage());
        }
    }
}
