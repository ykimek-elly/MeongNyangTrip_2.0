package com.team.meongnyang.recommendation.weather.service;

import com.team.meongnyang.recommendation.weather.dto.WeatherApiResponse;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

  @Value("${WEATHER_API_KEY:}")
  private String weatherApiKey;

  private static final String HOST = "apis.data.go.kr";
  private static final String WEATHER_PATH = "/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";

  private final RestClient restClient;
  private final WeatherRuleService ruleService;

//  @PostConstruct
//  public void checkEnv() {
//    System.out.println("System.getenv WEATHER_API_KEY = " + System.getenv("WEATHER_API_KEY"));
//    System.out.println("Injected weather.api.key length = " + (weatherApiKey == null ? "null" : weatherApiKey.length()));
//  }

  public WeatherContext getWeather(int nx, int ny) {

    if (weatherApiKey == null || weatherApiKey.isBlank()) {
      log.warn("[WEATHER API] WEATHER_API_KEY가 설정되지 않았습니다. fallback weather 사용");
      return fallbackWeather();
    }

    try {
      WeatherApiResponse response = requestWeather(nx, ny); // 호출
      List<WeatherItem> items = response.getResponse().getBody().getItems().getItem();
      Map<String, String> weatherMap = weatherMap(items);
      WeatherContext weatherContext = buildWeatherContext(weatherMap);
      log.info("[날씨 서비스] 날씨 요약 정보 nx={}, ny={}, temp={}, humidity={}, precipitationType={}, walkLevel={}",
              nx,
              ny,
              weatherContext.getTemperature(),
              weatherContext.getHumidity(),
              weatherContext.getPrecipitationType(),
              weatherContext.getWalkLevel());
      return weatherContext;

    } catch (Exception e) {
        log.error("[WEATHER API ERROR] nx={}, ny={}, msg={}", nx, ny, e.getMessage());
        return fallbackWeather();
    }
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
    log.warn("[날씨 서비스] fallback 날씨 반환 walkLevel={}, precipitationType={}",
            weatherContext.getWalkLevel(),
            weatherContext.getPrecipitationType());
    return weatherContext;
  }

  private WeatherApiResponse requestWeather(int nx, int ny) {
    String baseDate = getBaseDate();
    String baseTime = getBaseTime();

    return restClient.get()
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
            .body(WeatherApiResponse.class);
  }

  private String getBaseDate() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
  }

  private String getBaseTime() {
    return LocalTime.now()
            .minusHours(1)
            .format(DateTimeFormatter.ofPattern("HH00"));
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
