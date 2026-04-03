package com.team.meongnyang.recommendation.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 중복 추천 제어를 위한 Redis 상태를 관리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationDedupService {

    private static final int RECENT_HISTORY_LIMIT = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RecommendationCachePolicy recommendationCachePolicy;

    /**
     * 같은 사용자에 대한 동시 추천 요청을 잠근다.
     */
    public boolean tryAcquireUserRequestLock(Long userId) {
        String key = "recommendation:lock:" + userId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                recommendationCachePolicy.recommendationRequestLockTtl()
        );
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 사용자 추천 요청 락을 해제한다.
     */
    public void releaseUserRequestLock(Long userId) {
        if (userId == null) {
            return;
        }

        String key = "recommendation:lock:" + userId;
        redisTemplate.delete(key);
    }

    /**
     * 마지막으로 추천한 장소 ID를 조회한다.
     */
    public Long getLastRecommendedPlaceId(Long userId) {
        return toLong(redisTemplate.opsForValue().get(lastPlaceKey(userId)));
    }

    /**
     * 최근 추천 목록에 특정 장소가 포함되어 있는지 확인한다.
     */
    public boolean isRecentlyRecommended(Long userId, Long placeId) {
        if (placeId == null) {
            return false;
        }
        List<Object> recent = redisTemplate.opsForList().range(historyKey(userId), 0, RECENT_HISTORY_LIMIT - 1);
        return recent != null && recent.stream().anyMatch(value -> placeId.equals(toLong(value)));
    }

    /**
     * 최근 추천 이력을 갱신한다.
     */
    public void recordRecommendation(Long userId, Long placeId) {
        if (userId == null || placeId == null) {
            return;
        }

        redisTemplate.opsForValue().set(
                lastPlaceKey(userId),
                placeId,
                recommendationCachePolicy.recommendationHistoryTtl()
        );
        redisTemplate.opsForList().leftPush(historyKey(userId), placeId);
        redisTemplate.opsForList().trim(historyKey(userId), 0, RECENT_HISTORY_LIMIT - 1);
        redisTemplate.expire(historyKey(userId), recommendationCachePolicy.recommendationHistoryTtl());
    }

    private String lastPlaceKey(Long userId) {
        return "recommendation:last-place:" + userId;
    }

    private String historyKey(Long userId) {
        return "recommendation:last-places:" + userId;
    }

    private Long toLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
