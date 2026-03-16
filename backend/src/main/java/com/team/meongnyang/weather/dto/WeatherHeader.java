package com.team.meongnyang.weather.dto;


import lombok.Getter;
import lombok.Setter;

/**
 * 기상청 응답 header 영역 DTO
 *
 * API 호출 성공 여부를 확인하는 코드와 메시지를 담는다.
 */

@Getter
@Setter
public class WeatherHeader {

  /** 결과 코드 (00 = 정상) */
  private String resultCode;
  /** 결과 메시지 */
  private String resultMsg;
}