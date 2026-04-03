package com.team.meongnyang.recommendation.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.dto.RecommendationResultCachePayload;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 최종 추천 결과 캐시를 담당한다.
 *
 * Weather, 후보 장소, 반려동물 특성, 최근 추천 이력을 조합한 키를 받아
 * 짧은 시간 동안 같은 결과를 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationResultCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RecommendationCachePolicy recommendationCachePolicy;

    /**
     * 추천 결과를 조회한다.
     *
     * @param key 추천 컨텍스트 기반 Redis 키
     * @return 캐시된 추천 결과, 없으면 {@code null}
     */
    public RecommendationNotificationResult get(String key) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return null;
        }

        RecommendationResultCachePayload payload =
                objectMapper.convertValue(cached, RecommendationResultCachePayload.class);
        RecommendationNotificationResult result = payload.toNotificationResult();
        return result;
    }

    /**
     * 추천 결과를 저장한다.
     *
     * @param key 추천 컨텍스트 기반 Redis 키
     * @param result 저장할 추천 결과
     */
    public void save(String key, RecommendationNotificationResult result) {
        RecommendationResultCachePayload payload = RecommendationResultCachePayload.from(result);
        redisTemplate.opsForValue().set(
                key,
                payload,
                recommendationCachePolicy.recommendationResultTtl()
        );
    }
}
