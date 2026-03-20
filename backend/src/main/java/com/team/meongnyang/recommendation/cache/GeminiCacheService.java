package com.team.meongnyang.recommendation.cache;

import com.team.meongnyang.recommendation.service.GeminiRecommendationService;
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
 * 프롬프트 기반 Gemini 응답을 캐시해서 동일한 추천 요청의 AI 재호출을 줄이는 캐시 서비스이다.
 *
 * <p>파이프라인에서 프롬프트 생성 직후 캐시 키를 만들고 조회한다.
 * 캐시에 값이 있으면 즉시 응답을 재사용하고, 없을 때만 Gemini를 호출한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiCacheService {

  @Value("${redis.gemini-cache-ttl}")
  private Duration GEMINI_CACHE_TTL;
  @Value("${redis.gemini-cache-key}")
  private String GEMINI_CACHE_KEY_PREFIX;


  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 모델 버전과 프롬프트 내용을 함께 반영해 Gemini 응답 캐시 키를 생성한다.
   *
   * @param prompt 추천 문장 생성을 위해 구성한 최종 프롬프트
   * @return 동일 모델과 프롬프트 조합에 대응하는 Redis 캐시 키
   */
  public String generateKey(String prompt) {
    String key = GEMINI_CACHE_KEY_PREFIX + ":" + sha256(prompt);
    log.info("[Gemini 캐시] cache key 생성 promptLength={}, key={}", prompt == null ? 0 : prompt.length(), key);
    return key;
  }

  /**
   * 캐시 키로 저장된 Gemini 응답을 조회한다.
   *
   * @param key 프롬프트 해시가 포함된 Gemini 캐시 키
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
   * @param key 프롬프트 해시가 포함된 Gemini 캐시 키
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
