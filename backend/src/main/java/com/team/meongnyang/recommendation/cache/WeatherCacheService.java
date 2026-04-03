package com.team.meongnyang.recommendation.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 정규화된 날씨 정보를 Redis에 저장하고 재사용한다.
 *
 * 같은 격자 좌표에 대한 동시 요청이 몰릴 때 중복 API 호출이 발생하지 않도록
 * 키 단위 메모리 락을 함께 사용한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheService {

    @Value("${redis.weather-cache-key}")
    private String weatherCacheKeyPrefix;

    private final RedisTemplate<String, Object> redisTemplate;
    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    private final RecommendationCachePolicy recommendationCachePolicy;
    private final ConcurrentMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    /**
     * 날씨 캐시를 우선 조회하고, 없으면 외부 API를 호출해 적재한다.
     *
     * @param nx 기상청 격자 X 좌표
     * @param ny 기상청 격자 Y 좌표
     * @return 정규화된 날씨 컨텍스트
     */
    public WeatherContext getOrLoadWeather(int nx, int ny) {
        String key = buildCacheKey(nx, ny);
        WeatherContext cachedContext = readCachedWeather(key);
        if (cachedContext != null) {
            return cachedContext;
        }

        Object keyLock = keyLocks.computeIfAbsent(key, ignored -> new Object());
        try {
            synchronized (keyLock) {
                WeatherContext reloadedContext = readCachedWeather(key);
                if (reloadedContext != null) {
                    return reloadedContext;
                }

                WeatherContext weatherContext = weatherService.getWeather(nx, ny);
                log.info("[캐시] 날씨 저장 nx={}, ny={}, userId={}, petId={}, batchExecutionId={}, walkLevel={}, precipitationType={}",
                        nx,
                        ny,
                        RecommendationLogContext.userId(),
                        RecommendationLogContext.petId(),
                        RecommendationLogContext.batchExecutionId(),
                        weatherContext.getWalkLevel(),
                        weatherContext.getPrecipitationType());

                if (!"ERROR".equalsIgnoreCase(weatherContext.getWalkLevel())) {
                    Duration ttl = recommendationCachePolicy.weatherTtl(weatherContext);
                    redisTemplate.opsForValue().set(key, weatherContext, ttl);
                }

                return weatherContext;
            }
        } finally {
            keyLocks.remove(key, keyLock);
        }
    }

    /**
     * 격자 좌표 기반 캐시 키를 생성한다.
     *
     * @param nx 격자 X 좌표
     * @param ny 격자 Y 좌표
     * @return Redis 키
     */
    private String buildCacheKey(int nx, int ny) {
        return weatherCacheKeyPrefix + ":" + nx + ":" + ny;
    }

    /**
     * 캐시된 날씨 정보를 읽는다.
     *
     * @param key Redis 키
     * @return 캐시된 날씨 정보, 없으면 {@code null}
     */
    private WeatherContext readCachedWeather(String key) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return null;
        }

        WeatherContext weatherContext = objectMapper.convertValue(cached, WeatherContext.class);
        return weatherContext;
    }
}
