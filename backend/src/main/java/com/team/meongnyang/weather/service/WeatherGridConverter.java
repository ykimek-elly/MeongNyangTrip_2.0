package com.team.meongnyang.weather.service;

import com.team.meongnyang.weather.dto.WeatherGridPoint;
import org.springframework.stereotype.Component;

@Component
public class WeatherGridConverter {
  // 기상청 DFS 좌표변환 상수
  private static final double RE = 6371.00877; // 지구 반경(km)
  private static final double GRID = 5.0;      // 격자 간격(km)
  private static final double SLAT1 = 30.0;    // 투영 위도1(degree)
  private static final double SLAT2 = 60.0;    // 투영 위도2(degree)
  private static final double OLON = 126.0;    // 기준점 경도(degree)
  private static final double OLAT = 38.0;     // 기준점 위도(degree)
  private static final double XO = 43;         // 기준점 X좌표(GRID)
  private static final double YO = 136;        // 기준점 Y좌표(GRID)

  /**
   * 위도/경도를 기상청 격자 좌표(nx, ny)로 변환한다.
   *
   * @param lat 위도
   * @param lng 경도
   * @return 기상청 격자 좌표
   */
  public WeatherGridPoint convertToGrid(double lat, double lng) {
    double degrad = Math.PI / 180.0;

    double re = RE / GRID;
    double slat1 = SLAT1 * degrad;
    double slat2 = SLAT2 * degrad;
    double olon = OLON * degrad;
    double olat = OLAT * degrad;

    double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
            / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
    sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

    double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
    sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

    double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
    ro = re * sf / Math.pow(ro, sn);

    double ra = Math.tan(Math.PI * 0.25 + (lat) * degrad * 0.5);
    ra = re * sf / Math.pow(ra, sn);

    double theta = lng * degrad - olon;
    if (theta > Math.PI) theta -= 2.0 * Math.PI;
    if (theta < -Math.PI) theta += 2.0 * Math.PI;
    theta *= sn;

    int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
    int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

    return new WeatherGridPoint(nx, ny);
  }

}
