package com.team.meongnyang.recommendation.weather.service;

import org.springframework.stereotype.Component;

@Component
public class WeatherRuleService {
  /**
   * 비 또는 눈이 오는 상태인지 판별한다.
   *
   * @param ptyCode 강수 형태 코드
   * @param rainfall 1시간 강수량
   * @return 강수 여부
   */
  public boolean isRaining(String ptyCode, double rainfall) {
    return !"0".equals(ptyCode) || rainfall > 0;
  }

  /**
   * 저온 상태인지 판별한다.
   *
   * @param temperature 현재 기온
   * @return 저온 여부
   */
  public boolean isCold(double temperature) {
    return temperature <= 7.0;
  }

  /**
   * 고온 상태인지 판별한다.
   *
   * @param temperature 현재 기온
   * @return 고온 여부
   */
  public boolean isHot(double temperature) {
    return temperature >= 28.0;
  }

  /**
   * 강풍 상태인지 판별한다.
   *
   * @param windSpeed 현재 풍속
   * @return 강풍 여부
   */
  public boolean isWindy(double windSpeed) {
    return windSpeed >= 6.0;
  }

  /**
   * 날씨 상태를 기준으로 산책 가능 수준을 계산한다.
   *
   * @param raining 강수 여부
   * @param cold 저온 여부
   * @param hot 고온 여부
   * @param windy 강풍 여부
   * @return 산책 가능 수준
   */
  public String evaluateWalkLevel(boolean raining, boolean cold, boolean hot, boolean windy) {
    if (raining || hot || windy || cold) {
      return "CAUTION";
    }
    return "GOOD";
  }
}
