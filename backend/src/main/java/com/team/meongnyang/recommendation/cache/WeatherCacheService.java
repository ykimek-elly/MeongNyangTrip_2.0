package com.team.meongnyang.recommendation.cache;


import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 역할 :
 *
 */

/**
 * 날씨 조회 결과를 Redis에 캐시해 동일 격자 좌표의 반복 호출 비용을 줄이는 캐시 계층이다.
 *
 * <p>파이프라인 흐름에서는 격자 좌표 변환 직후 호출되며,
 * 캐시에 값이 있으면 즉시 재사용하고 없으면 {@link WeatherService}를 통해 조회한 뒤 저장한다.
 * 반환된 날씨 문맥은 후보 장소 필터링과 점수 계산의 입력으로 이어진다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheService {

  @Value("${redis.weather-cache-key}")
  private String WEATHER_CACHE_KEY_PREFIX;
  @Value("${redis.weather-cache-ttl}")
  private Duration WEATHER_CACHE_TTL;

  private final RedisTemplate<String, Object> redisTemplate;
  private final WeatherService weatherService;
  private final ObjectMapper objectMapper;
  private final ConcurrentMap<String, Object> keyLocks = new ConcurrentHashMap<>();

  /**
   * 격자 좌표 기준 날씨 문맥을 캐시에서 우선 조회하고, 없으면 외부 조회 후 저장한다.
   *
   * @param nx 기상청 격자 X 좌표
   * @param ny 기상청 격자 Y 좌표
   * @return 추천 흐름에서 재사용할 현재 날씨 문맥
   */
  public WeatherContext getOrLoadWeather (int nx, int ny) {
    String key = buildCacheKey(nx, ny);
    log.info("[날씨 캐시] cache check key={}", key);
    // Cache 확인
    WeatherContext cachedContext = readCachedWeather(key);
    // Cache Hit
    if (cachedContext != null) {
      return cachedContext;
    }
    // Cache Miss

    // 이 캐시 key 전용 lock 객체를 가져온다 이때 없으면 새로 만든다
    Object keyLock = keyLocks.computeIfAbsent(key, ignored -> new Object());
    try {
      // 이 key에 대해서는 한 번에 한 스레드만 처리하게 한다
      synchronized (keyLock) {
        // lock 기다리는 동안 다른 스레드가 캐시를 채웠는지 다시 확인한다
        WeatherContext reloadedContext = readCachedWeather(key);
        if (reloadedContext != null) {
          return reloadedContext;
        }

        // Cache Miss -> API 호출
        WeatherContext weatherContext = weatherService.getWeather(nx, ny);
        log.info("[날씨 캐시] CACHE MISS 결과 walkLevel={}, precipitationType={}",
                weatherContext.getWalkLevel(),
                weatherContext.getPrecipitationType());

        // 장애 fallback 응답은 짧게 소비하고 다시 시도할 수 있게 캐시에 저장하지 않는다.
        if (!"ERROR".equalsIgnoreCase(weatherContext.getWalkLevel())) {
          redisTemplate.opsForValue().set(key, weatherContext, WEATHER_CACHE_TTL);
          log.info("[날씨 캐시] CACHE SAVE key={}, ttl={}min", key, WEATHER_CACHE_TTL.toMinutes());
        }

        return weatherContext;
      }
    } finally {
      keyLocks.remove(key, keyLock);
    }
  }

  /**
   * WEATHER CACHE KEY 생성
   */
  private String buildCacheKey(int nx, int ny) {
    return WEATHER_CACHE_KEY_PREFIX + ":" + nx + ":" + ny;
  }

  /**
   * 캐시에서 날씨 정보를 읽어온다.
   * @param key 날씨 정보를 저장한 Redis 키
   * @return 캐시에 저장된 날씨 정보, 캐시 미스인 경우 null
   */
  private WeatherContext readCachedWeather(String key) {
    // redis 조회
    Object cached = redisTemplate.opsForValue().get(key);
    // Cache miss -> return
    if (cached == null) {
      return null;
    }
    // Cache hit -> return WeatherContext
    WeatherContext weatherContext = objectMapper.convertValue(cached, WeatherContext.class);
    log.info("[날씨 캐시] CACHE HIT 결과 walkLevel={}, precipitationType={}",
            weatherContext.getWalkLevel(),
            weatherContext.getPrecipitationType());
    return weatherContext;
  }
}
