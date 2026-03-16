package com.team.meongnyang.recommendation.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final Duration GEMINI_CACHE_TTL = Duration.ofHours(1);

  public String generateKey(String prompt) {
    String key = "gemini:" + sha256(prompt);
    log.info("[Gemini 캐시] cache key 생성 promptLength={}, key={}", prompt == null ? 0 : prompt.length(), key);
    return key;
  }

  public String get(String key) {
    Object cached = redisTemplate.opsForValue().get(key);

    if (cached instanceof String value) {
      log.info("[Gemini 캐시] CACHE HIT key={}", key);
      return value;
    }

    log.info("[Gemini 캐시] CACHE MISS key={}", key);
    return null;
  }

  public void save(String key, String response) {
    redisTemplate.opsForValue().set(key, response, GEMINI_CACHE_TTL);
    log.info("[Gemini 캐시] CACHE SAVE key={}, ttl={}min", key, GEMINI_CACHE_TTL.toMinutes());
  }

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
      throw new IllegalStateException("SHA-256 해시 생성 실패", e);
    }
  }
}
