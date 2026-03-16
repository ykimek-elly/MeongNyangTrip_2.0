package com.team.meongnyang.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeatherGridPoint {
  private final int nx;
  private final int ny;
}