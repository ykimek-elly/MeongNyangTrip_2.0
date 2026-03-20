package com.team.meongnyang.recommendation.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.dto.DailyRecommendationCachePayload;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyRecommendationCacheService {

  @Value("${redis.daily-recommendation-cache-ttl}")
  private Duration DAILY_RECOMMENDATION_TTL;
  @Value("${redis.daily-recommendation-cache-key}")
  private String DAILY_RECOMMENDATION_KEY_PREFIX;

  private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
  private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  public void saveToday(Long userId, RecommendationNotificationResult result, String batchExecutionId) {
    LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
    String dateKey = formatDateKey(now.toLocalDate());
    String key = buildKey(userId, dateKey);

    DailyRecommendationCachePayload payload = DailyRecommendationCachePayload.from(
            result,
            dateKey,
            batchExecutionId,
            now
    );

    redisTemplate.opsForValue().set(key, payload, DAILY_RECOMMENDATION_TTL);
    log.info("[DailyRecommendationCache] CACHE SAVE key={}, ttlHours={}", key, DAILY_RECOMMENDATION_TTL.toHours());
  }

  public RecommendationNotificationResult getTodayResult(Long userId) {
    return getResult(userId, LocalDate.now(SEOUL_ZONE));
  }

  public RecommendationNotificationResult getResult(Long userId, LocalDate date) {
    String key = buildKey(userId, formatDateKey(date));
    Object cached = redisTemplate.opsForValue().get(key);
    if (cached == null) {
      log.info("[DailyRecommendationCache] CACHE MISS key={}", key);
      return null;
    }

    DailyRecommendationCachePayload payload = objectMapper.convertValue(cached, DailyRecommendationCachePayload.class);
    log.info("[DailyRecommendationCache] CACHE HIT key={}, placeId={}",
            key,
            payload.getPlace() == null ? null : payload.getPlace().getId());
    return payload.toNotificationResult();
  }

  public boolean isSentToday(LocalDateTime lastNotificationSentAt) {
    if (lastNotificationSentAt == null) {
      return false;
    }
    return lastNotificationSentAt.toLocalDate().isEqual(LocalDate.now(SEOUL_ZONE));
  }

  private String buildKey(Long userId, String dateKey) {
    return DAILY_RECOMMENDATION_KEY_PREFIX + ":" + userId + ":" + dateKey;
  }

  private String formatDateKey(LocalDate date) {
    return date.format(DATE_KEY_FORMATTER);
  }
}
