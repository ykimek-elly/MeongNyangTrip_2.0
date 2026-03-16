package com.team.meongnyang.weather.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


/**
 * 기상청 응답의 item 배열을 감싸는 DTO
 *
 * 실제 날씨 데이터는 item 리스트 안에 여러 개의 WeatherItem 형태로 존재한다.
 */
@Getter
@Setter
public class WeatherItems {
  /** 날씨 항목 리스트 */
  private List<WeatherItem> item;
}
