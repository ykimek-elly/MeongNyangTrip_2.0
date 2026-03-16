package com.team.meongnyang.orchestrator.service;

import org.springframework.stereotype.Component;

@Component
public class CalcDistanceService {
  /**
   * 두 좌표 간 거리를 km 단위로 계산한다.
   *
   * Haversine 공식을 사용하여 지구 곡률을 반영한 직선거리를 구한다.
   *
   * @param lat1 사용자 위도
   * @param lng1 사용자 경도
   * @param lat2 장소 위도
   * @param lng2 장소 경도
   * @return 거리 (km)
   */
  public double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
    final int earthRadius = 6371; // 지구 반지름 (km)

    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return earthRadius * c;
  }
}
