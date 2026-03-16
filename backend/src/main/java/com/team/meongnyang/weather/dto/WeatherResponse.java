package com.team.meongnyang.weather.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기상청 response 영역 DTO
 *
 * header 와 body 를 함께 포함한다.
 */

@Getter
@Setter
public class WeatherResponse {

  /** 응답 상태 정보 */
  private WeatherHeader header;
  /** 실제 날씨 데이터 영역 */
  private WeatherBody body;
}