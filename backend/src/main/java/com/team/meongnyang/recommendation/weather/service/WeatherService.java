package com.team.meongnyang.recommendation.weather.service;

import com.team.meongnyang.recommendation.weather.dto.WeatherApiResponse;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherItem;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 기상청 초단기 실황 API를 호출해 추천 흐름에서 사용할 날씨 문맥을 만드는 서비스이다.
 *
 * <p>추천 파이프라인에서 격자 좌표가 계산된 뒤 호출되며,
 * API 응답을 산책 가능 수준과 강수 여부 같은 추천 판단용 {@link WeatherContext}로 변환한다.
 * 생성된 결과는 후보 장소 필터링, 점수 계산, 프롬프트 생성의 공통 입력으로 사용된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

  @Value("${WEATHER_API_KEY:}")
  private String weatherApiKey;

  @Value("${recommendation.weather.timeout-ms:1500}")
  private long weatherTimeoutMs;

  @Value("${recommendation.weather.retry.max-attempts:2}")
  private int weatherMaxAttempts;

  @Value("${recommendation.weather.retry.backoff-ms:200}")
  private long weatherRetryBackoffMs;

  private static final String HOST = "apis.data.go.kr";
  private static final String WEATHER_PATH = "/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";

  private final RestClient restClient;
  private final WeatherRuleService ruleService;
  private final ExecutorService weatherCallExecutor = Executors.newVirtualThreadPerTaskExecutor();

//  @PostConstruct
//  public void checkEnv() {
//    System.out.println("System.getenv WEATHER_API_KEY = " + System.getenv("WEATHER_API_KEY"));
//    System.out.println("Injected weather.api.key length = " + (weatherApiKey == null ? "null" : weatherApiKey.length()));
//  }

  /**
   * 지정한 기상청 격자 좌표의 현재 날씨를 조회해 추천용 날씨 문맥으로 변환한다.
   *
   * @param nx 기상청 격자 X 좌표
   * @param ny 기상청 격자 Y 좌표
   * @return 추천 필터링과 점수 계산에 사용할 날씨 문맥, 조회 실패 시 fallback 날씨 문맥
   */
  public WeatherContext getWeather(int nx, int ny) {

    if (weatherApiKey == null || weatherApiKey.isBlank()) {
      log.warn("[날씨 조회] API 키 없음 fallback 사용 nx={}, ny={}, userId={}, petId={}, batchExecutionId={}",
              nx,
              ny,
              RecommendationLogContext.userId(),
              RecommendationLogContext.petId(),
              RecommendationLogContext.batchExecutionId());
      return fallbackWeather();
    }

    for (int attempt = 1; attempt <= Math.max(weatherMaxAttempts, 1); attempt++) {
      try {
        WeatherApiResponse response = requestWeather(nx, ny); // 호출
        List<WeatherItem> items = response.getResponse().getBody().getItems().getItem();
        if (items == null || items.isEmpty()) {
          throw new IllegalStateException("weather items is empty");
        }

        Map<String, String> weatherMap = weatherMap(items);
        WeatherContext weatherContext = buildWeatherContext(weatherMap);
        log.info("[날씨 조회] 조회 완료 nx={}, ny={}, userId={}, petId={}, batchExecutionId={}, temp={}, humidity={}, precipitationType={}, walkLevel={}, attempt={}",
                nx,
                ny,
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                weatherContext.getTemperature(),
                weatherContext.getHumidity(),
                weatherContext.getPrecipitationType(),
                weatherContext.getWalkLevel(),
                attempt);
        return weatherContext;
      } catch (Exception e) {
        log.warn("[날씨 조회] 재시도 nx={}, ny={}, userId={}, petId={}, batchExecutionId={}, attempt={}, maxAttempts={}, reason={}",
                nx,
                ny,
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                attempt,
                weatherMaxAttempts,
                e.getMessage());

        if (attempt >= Math.max(weatherMaxAttempts, 1)) {
          log.error("[에러] 날씨 조회 실패 nx={}, ny={}, userId={}, petId={}, batchExecutionId={}, reason={}",
                  nx,
                  ny,
                  RecommendationLogContext.userId(),
                  RecommendationLogContext.petId(),
                  RecommendationLogContext.batchExecutionId(),
                  e.getMessage(),
                  e);
          return fallbackWeather();
        }

        sleepBeforeRetry();
      }
    }

    return fallbackWeather();
  }

  private WeatherContext fallbackWeather() {
    WeatherContext weatherContext = WeatherContext.builder()
            .temperature(0)
            .humidity(0)
            .precipitationType("ERROR")
            .rainfall(0)
            .windSpeed(0)
            .raining(false)
            .cold(false)
            .hot(false)
            .windy(false)
            .walkLevel("ERROR")
            .build();
    log.warn("[날씨 조회] fallback 반환 userId={}, petId={}, batchExecutionId={}, walkLevel={}, precipitationType={}",
            RecommendationLogContext.userId(),
            RecommendationLogContext.petId(),
            RecommendationLogContext.batchExecutionId(),
            weatherContext.getWalkLevel(),
            weatherContext.getPrecipitationType());
    return weatherContext;
  }

  private WeatherApiResponse requestWeather(int nx, int ny) {
    String baseDate = getBaseDate();
    String baseTime = getBaseTime();
    Future<WeatherApiResponse> future = weatherCallExecutor.submit(() -> restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(HOST)
                    .path(WEATHER_PATH)
                    .queryParam("serviceKey", weatherApiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 20)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build())
            .retrieve()
            .body(WeatherApiResponse.class));

    try {
      return future.get(weatherTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new IllegalStateException("weather api timeout", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("weather api interrupted", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("weather api failed", e.getCause());
    }
  }

  public String getBaseDate() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
  }

  public String getBaseTime() {
    return LocalTime.now()
            .minusHours(1)
            .format(DateTimeFormatter.ofPattern("HH00"));
  }

  /**
   * 재시도 간격은 짧게 유지해서 외부 API 지연이 전체 배치 시간으로 전파되지 않도록 한다.
   */
  private void sleepBeforeRetry() {
    try {
      Thread.sleep(Math.max(weatherRetryBackoffMs, 0L));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @PreDestroy
  void shutdownExecutor() {
    weatherCallExecutor.close();
  }

  private Map<String, String> weatherMap(List<WeatherItem> items) {
    Map<String, String> weatherMap = new HashMap<>();

    for (WeatherItem item : items) {
      weatherMap.put(item.getCategory(), item.getObsrValue());
    }

    return weatherMap;
  }

  private WeatherContext buildWeatherContext(Map<String, String> weatherMap) {
    double temperature = parseDouble(weatherMap.get("T1H"));
    int humidity = parseInt(weatherMap.get("REH"));
    String ptyCode = weatherMap.getOrDefault("PTY", "0");
    double rainfall = parseDouble(weatherMap.get("RN1"));
    double windSpeed = parseDouble(weatherMap.get("WSD"));

    String precipitationType = convertPty(ptyCode);

    boolean raining = ruleService.isRaining(ptyCode, rainfall);
    boolean cold = ruleService.isCold(temperature);
    boolean hot = ruleService.isHot(temperature);
    boolean windy = ruleService.isWindy(windSpeed);

    String walkLevel = ruleService.evaluateWalkLevel(raining, cold, hot, windy);

    return WeatherContext.builder()
            .temperature(temperature)
            .humidity(humidity)
            .precipitationType(precipitationType)
            .rainfall(rainfall)
            .windSpeed(windSpeed)
            .raining(raining)
            .cold(cold)
            .hot(hot)
            .windy(windy)
            .walkLevel(walkLevel)
            .build();
  }

  private double parseDouble(String value) {
    if (value == null || value.isBlank()) {
      return 0.0;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  private int parseInt(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return (int) Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private String convertPty(String value) {
    return switch (value) {
      case "0" -> "없음";
      case "1" -> "비";
      case "2" -> "비/눈";
      case "3" -> "눈";
      case "4" -> "소나기";
      default -> "알 수 없음";
    };
  }
}
