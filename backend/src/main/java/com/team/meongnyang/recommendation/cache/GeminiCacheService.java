package com.team.meongnyang.recommendation.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * Gemini 응답 캐시를 담당한다.
 *
 * <p>최종 프롬프트 기준 캐시와 정규화된 컨텍스트 기준 캐시를 함께 제공한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiCacheService {

    @Value("${redis.gemini-cache-ttl}")
    private Duration geminiCacheTtl;

    @Value("${redis.gemini-cache-key}")
    private String geminiCacheKeyPrefix;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RecommendationCachePolicy recommendationCachePolicy;

    /**
     * 최종 프롬프트 기준 캐시 키를 생성한다.
     *
     * @param prompt Gemini 최종 프롬프트
     * @return Redis 키
     */
    public String generateKey(String prompt) {
        String key = geminiCacheKeyPrefix + ":" + sha256(prompt);
        log.info("[GeminiCache] 프롬프트 키 생성 key={}", key);
        return key;
    }

    /**
     * 공용 컨텍스트 기준 캐시 키를 생성한다.
     *
     * @param fingerprint 정규화된 컨텍스트 fingerprint
     * @return Redis 키
     */
    public String generateContextKey(String fingerprint) {
        String key = geminiCacheKeyPrefix + ":context:" + sha256(fingerprint);
        log.info("[GeminiCache] 컨텍스트 키 생성 key={}", key);
        return key;
    }

    /**
     * 캐시된 Gemini 응답을 조회한다.
     *
     * @param key Redis 키
     * @return 캐시된 응답, 없으면 {@code null}
     */
    public String get(String key) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof String value) {
            log.info("[GeminiCache] CACHE HIT key={}", key);
            return value;
        }
        log.info("[GeminiCache] CACHE MISS key={}", key);
        return null;
    }

    /**
     * 최종 프롬프트 기준 응답을 저장한다.
     *
     * @param key Redis 키
     * @param response Gemini 응답
     */
    public void save(String key, String response) {
        redisTemplate.opsForValue().set(key, response, geminiCacheTtl);
        log.info("[GeminiCache] CACHE SAVE key={}, ttlMinutes={}", key, geminiCacheTtl.toMinutes());
    }

    /**
     * 공용 컨텍스트 기준 응답을 저장한다.
     *
     * @param key Redis 키
     * @param response Gemini 응답
     */
    public void saveContext(String key, String response) {
        redisTemplate.opsForValue().set(key, response, recommendationCachePolicy.geminiContextCacheTtl());
        log.info("[GeminiCache] CONTEXT CACHE SAVE key={}, ttlMinutes={}",
                key,
                recommendationCachePolicy.geminiContextCacheTtl().toMinutes());
    }

    /**
     * 문자열을 SHA-256 해시로 변환한다.
     *
     * @param value 원본 문자열
     * @return 16진수 해시 문자열
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", e);
        }
    }
}
