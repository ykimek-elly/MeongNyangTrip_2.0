package com.team.meongnyang.recommendation.cache;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


/**
 * 역할 :
 *
 */

/**
 * 날씨 조회 결과를 Redis에 캐시해 동일 격자 좌표의 반복 호출 비용을 줄이는 캐시 계층이다.
 *
 * <p>오케스트레이션 흐름에서는 격자 좌표 변환 직후 호출되며,
 * 캐시에 값이 있으면 즉시 재사용하고 없으면 {@link WeatherService}를 통해 조회한 뒤 저장한다.
 * 반환된 날씨 문맥은 후보 장소 필터링과 점수 계산의 입력으로 이어진다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final WeatherService weatherService;
  private final ObjectMapper objectMapper;
  private static final Duration WEATHER_CACHE_TTL = Duration.ofHours(3);

  /**
   * 격자 좌표 기준 날씨 문맥을 캐시에서 우선 조회하고, 없으면 외부 조회 후 저장한다.
   *
   * @param nx 기상청 격자 X 좌표
   * @param ny 기상청 격자 Y 좌표
   * @return 추천 흐름에서 재사용할 현재 날씨 문맥
   */
  public WeatherContext getOrLoadWeather (int nx, int ny) {
    String key = "weather:" + nx + ":" + ny;
    log.info("[날씨 캐시] cache check key={}", key);
    // redis 조회
    Object cached = redisTemplate.opsForValue().get(key);

    // Cache Hit -> return
    if (cached != null) {
      // WeatherContext로 변환
      WeatherContext weatherContext = objectMapper.convertValue(cached, WeatherContext.class);
      log.info("[날씨 캐시] CACHE HIT 결과 walkLevel={}, precipitationType={}",
              weatherContext.getWalkLevel(),
              weatherContext.getPrecipitationType());
      return weatherContext;
    }

    // Cache Miss -> API 호출
    WeatherContext weatherContext = weatherService.getWeather(nx, ny);
    log.info("[날씨 캐시] CACHE MISS 결과 walkLevel={}, precipitationType={}",
            weatherContext.getWalkLevel(),
            weatherContext.getPrecipitationType());

    // Redis 저장
    redisTemplate.opsForValue().set(key, weatherContext, WEATHER_CACHE_TTL);
    log.info("[날씨 캐시] CACHE SAVE key={}, ttl={}min", key, WEATHER_CACHE_TTL.toMinutes());

    return weatherContext;
  }

}
