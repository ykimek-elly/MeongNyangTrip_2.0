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

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final WeatherService weatherService;
  private final ObjectMapper objectMapper;
  private static final Duration WEATHER_CACHE_TTL = Duration.ofMinutes(15);

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
