package com.team.meongnyang.weather.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기상청 API 최상위 응답 DTO
 *
 * 전체 응답의 최상단 response 객체를 감싼다.
 */

@Getter
@Setter
public class WeatherApiResponse {

  private WeatherResponse response;
}