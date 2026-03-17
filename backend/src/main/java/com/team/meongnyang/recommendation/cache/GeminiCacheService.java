package com.team.meongnyang.recommendation.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * 프롬프트 기반 Gemini 응답을 캐시해 동일 추천 요청의 AI 재호출을 줄이는 캐시 계층이다.
 *
 * <p>파이프라인에서 프롬프트 생성 직후 캐시 키를 만들고 조회하는 단계에 사용되며,
 * 캐시 적중 시 즉시 추천 문장을 반환하고 미적중 시 새로 생성한 응답을 저장한다.
 * 이 결과는 응답 지연 감소와 동일 입력 재사용에 활용된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final Duration GEMINI_CACHE_TTL = Duration.ofHours(1);

  /**
   * 프롬프트 내용을 기반으로 Gemini 응답 캐시 키를 생성한다.
   *
   * @param prompt 추천 문장 생성을 위해 구성한 최종 프롬프트
   * @return 동일한 프롬프트에 대해 재사용 가능한 Redis 캐시 키
   */
  public String generateKey(String prompt) {
    String key = "gemini:" + sha256(prompt);
    log.info("[Gemini 캐시] cache key 생성 promptLength={}, key={}", prompt == null ? 0 : prompt.length(), key);
    return key;
  }

  /**
   * 캐시 키로 저장된 Gemini 응답을 조회한다.
   *
   * @param key 프롬프트 해시를 포함한 Gemini 캐시 키
   * @return 캐시에 저장된 추천 문장, 없으면 {@code null}
   */
  public String get(String key) {
    Object cached = redisTemplate.opsForValue().get(key);

    if (cached instanceof String value) {
      log.info("[Gemini 캐시] CACHE HIT key={}", key);
      return value;
    }

    log.info("[Gemini 캐시] CACHE MISS key={}", key);
    return null;
  }

  /**
   * 새로 생성한 Gemini 응답을 캐시에 저장한다.
   *
   * @param key 프롬프트 해시를 포함한 Gemini 캐시 키
   * @param response 사용자에게 반환할 Gemini 추천 문장
   */
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
