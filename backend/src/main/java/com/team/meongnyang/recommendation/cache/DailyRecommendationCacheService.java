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

/**
 * 하루 단위 추천 결과와 발송 마커를 관리한다.
 *
 * <p>배치 발송 후 앱 조회에서도 같은 결과를 재사용할 수 있도록
 * 추천 payload와 sent marker를 함께 저장한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyRecommendationCacheService {

    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Value("${redis.daily-recommendation-cache-ttl}")
    private Duration dailyRecommendationTtl;

    @Value("${redis.daily-recommendation-cache-key}")
    private String dailyRecommendationKeyPrefix;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RecommendationCachePolicy recommendationCachePolicy;

    /**
     * 오늘자 추천 결과를 저장하고 발송 마커도 함께 기록한다.
     *
     * @param userId 사용자 ID
     * @param result 추천 결과
     * @param batchExecutionId 배치 실행 ID
     */
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

        redisTemplate.opsForValue().set(key, payload, recommendationCachePolicy.ttlUntilTomorrow(now));
        markSent(userId, now);
        log.info("[DailyRecommendationCache] CACHE SAVE key={}, ttlHours={}",
                key,
                recommendationCachePolicy.ttlUntilTomorrow(now).toHours());
    }

    /**
     * 오늘자 추천 결과를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 추천 결과, 없으면 {@code null}
     */
    public RecommendationNotificationResult getTodayResult(Long userId) {
        return getResult(userId, LocalDate.now(SEOUL_ZONE));
    }

    /**
     * 특정 날짜의 추천 결과를 조회한다.
     *
     * @param userId 사용자 ID
     * @param date 조회 날짜
     * @return 추천 결과, 없으면 {@code null}
     */
    public RecommendationNotificationResult getResult(Long userId, LocalDate date) {
        String key = buildKey(userId, formatDateKey(date));
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            log.info("[DailyRecommendationCache] CACHE MISS key={}", key);
            return null;
        }

        DailyRecommendationCachePayload payload =
                objectMapper.convertValue(cached, DailyRecommendationCachePayload.class);
        log.info("[DailyRecommendationCache] CACHE HIT key={}, placeId={}",
                key,
                payload.getPlace() == null ? null : payload.getPlace().getId());
        return payload.toNotificationResult();
    }

    /**
     * DB timestamp만 기준으로 오늘 발송 여부를 판단한다.
     *
     * @param lastNotificationSentAt DB의 마지막 발송 시각
     * @return 오늘 발송 여부
     */
    public boolean isSentToday(LocalDateTime lastNotificationSentAt) {
        if (lastNotificationSentAt == null) {
            return false;
        }
        return lastNotificationSentAt.toLocalDate().isEqual(LocalDate.now(SEOUL_ZONE));
    }

    /**
     * Redis sent marker를 우선 사용하고, 없으면 DB timestamp를 확인한다.
     *
     * @param userId 사용자 ID
     * @param lastNotificationSentAt DB의 마지막 발송 시각
     * @return 오늘 발송 여부
     */
    public boolean isSentToday(Long userId, LocalDateTime lastNotificationSentAt) {
        Object cached = redisTemplate.opsForValue().get(buildSentKey(userId, LocalDate.now(SEOUL_ZONE)));
        if (cached != null) {
            return true;
        }
        return isSentToday(lastNotificationSentAt);
    }

    /**
     * 오늘자 발송 마커를 기록한다.
     *
     * @param userId 사용자 ID
     * @param sentAt 발송 시각
     */
    public void markSent(Long userId, LocalDateTime sentAt) {
        redisTemplate.opsForValue().set(
                buildSentKey(userId, sentAt.toLocalDate()),
                "1",
                recommendationCachePolicy.ttlUntilTomorrow(sentAt)
        );
    }

    /**
     * 추천 결과 캐시 키를 만든다.
     *
     * @param userId 사용자 ID
     * @param dateKey 날짜 키
     * @return Redis 키
     */
    private String buildKey(Long userId, String dateKey) {
        return dailyRecommendationKeyPrefix + ":" + userId + ":" + dateKey;
    }

    /**
     * 날짜를 Redis 키용 문자열로 변환한다.
     *
     * @param date 날짜
     * @return yyyyMMdd 형식 문자열
     */
    private String formatDateKey(LocalDate date) {
        return date.format(DATE_KEY_FORMATTER);
    }

    /**
     * 발송 마커 키를 생성한다.
     *
     * @param userId 사용자 ID
     * @param date 날짜
     * @return Redis 키
     */
    private String buildSentKey(Long userId, LocalDate date) {
        return "notification:sent:" + userId + ":" + formatDateKey(date);
    }
}
