package com.team.meongnyang.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 Refresh Token 저장/검증/삭제 서비스.
 * Key 형식: "refresh:{email}"
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    /** Refresh Token을 Redis에 저장 (TTL = 7일) */
    public void save(String email, String refreshToken) {
        redisTemplate.opsForValue().set(
                PREFIX + email,
                refreshToken,
                Duration.ofSeconds(jwtUtil.getRefreshExpirationSeconds())
        );
    }

    /**
     * Refresh Token 검증.
     * 1. JWT 자체 유효성 검사
     * 2. Redis에 저장된 토큰과 일치 여부 확인 (탈취/재사용 방지)
     */
    public boolean validate(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) return false;

        String email = jwtUtil.getEmailFromToken(refreshToken);
        String stored = redisTemplate.opsForValue().get(PREFIX + email);

        return refreshToken.equals(stored);
    }

    /** Refresh Token으로부터 이메일 추출 */
    public String getEmail(String refreshToken) {
        return jwtUtil.getEmailFromToken(refreshToken);
    }

    /** 로그아웃 시 Refresh Token 삭제 */
    public void delete(String email) {
        redisTemplate.delete(PREFIX + email);
    }
}
