package com.team.meongnyang.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위도(lat)와 경도(lng)를 표현하는 좌표 객체
 * 위치 기반 계산 및 거리 산정에 사용된다.
 */
@Getter
@NoArgsConstructor
public class Coordinate {
  private double lat;
  private double lng;
}