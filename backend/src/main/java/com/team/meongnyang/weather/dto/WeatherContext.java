package com.team.meongnyang.weather.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 서비스 내부에서 사용할 표준화된 날씨 정보 DTO
 *
 * 기상청 원본 응답을 그대로 사용하지 않고,
 * AI 프롬프트, 추천 알고리즘, 알림 서비스에서
 * 바로 활용할 수 있는 형태로 가공한 객체이다.
 */
@Getter
@Builder
@ToString
public class WeatherContext {
  /** 현재 기온(℃) */
  private double temperature;
  /** 현재 습도(%) */
  private int humidity;
  /** 강수 형태 (없음, 비, 눈, 비/눈, 소나기) */
  private String precipitationType;
  /** 1시간 강수량(mm) */
  private double rainfall;
  /** 현재 풍속(m/s) */
  private double windSpeed;

  /** 강수 여부 */
  private boolean raining;
  /** 저온 여부 */
  private boolean cold;
  /** 고온 여부 */
  private boolean hot;
  /** 강풍 여부 */
  private boolean windy;

  /** 산책 가능 수준 (GOOD / CAUTION / DANGEROUS) */
  private String walkLevel;
}