package com.team.meongnyang.weather.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기상청 초단기 실황 API의 개별 날씨 항목(item) 정보를 담는 DTO
 *
 * category 값에 따라 실제 의미가 달라진다.
 *
 * T1H : 현재 기온
 * REH : 습도
 * PTY : 강수 형태
 * RN1 : 1시간 강수량
 * WSD : 풍속
 *
 * obsrValue 에 실제 관측값이 저장된다.
 */

@Getter
@Setter
public class WeatherItem {
  /** 발표 기준 날짜 (yyyyMMdd) */
  private String baseDate;
  /** 발표 기준 시간 (HHmm) */
  private String baseTime;
  /** 날씨 카테고리 코드 */
  private String category;
  /** 기상청 X 좌표 */
  private int nx;
  /** 기상청 Y 좌표 */
  private int ny;
  /** 실제 관측값 */
  private String obsrValue;
}
