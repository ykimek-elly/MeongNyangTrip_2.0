package com.team.meongnyang.weather.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.weather.dto.WeatherContext;
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
    // redis 조회
    Object cached = redisTemplate.opsForValue().get(key);

    // Cache Hit -> return
    if (cached != null) {
      // WeatherContext로 변환
      WeatherContext weatherContext = objectMapper.convertValue(cached, WeatherContext.class);
      log.info("[WEATHER CACHE HIT] key={}", key);
      return weatherContext;
    }

    // Cache Miss -> API 호출
    log.info("[WEATHER CACHE MISS] key={}", key);
    WeatherContext weatherContext = weatherService.getWeather(nx, ny);

    // Redis 저장
    redisTemplate.opsForValue().set(key, weatherContext, WEATHER_CACHE_TTL);
    log.info("[WEATHER CACHE SAVE] key={}, ttl={}min", key, WEATHER_CACHE_TTL.toMinutes());

    return weatherContext;
  }
}
