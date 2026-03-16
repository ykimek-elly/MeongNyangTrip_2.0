package com.team.meongnyang.recommendation.weather.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기상청 응답 body 영역 DTO
 *
 * 날씨 데이터(items)와 페이지 정보가 포함된다.
 */


@Getter
@Setter
public class WeatherBody {
  /** 응답 데이터 타입 (JSON / XML) */
  private String dataType;
  /** 날씨 item 리스트 */
  private WeatherItems items;
  /** 페이지 번호 */
  private int pageNo;
  /** 요청 행 수 */
  private int numOfRows;
  /** 전체 데이터 개수 */
  private int totalCount;
}
