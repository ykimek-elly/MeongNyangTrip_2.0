package com.team.meongnyang.batch.notification;

import com.team.meongnyang.recommendation.cache.WeatherCacheService;
import com.team.meongnyang.recommendation.dto.Coordinate;
import com.team.meongnyang.recommendation.util.RegionCoordinateConfig;
import com.team.meongnyang.recommendation.weather.dto.WeatherGridPoint;
import com.team.meongnyang.recommendation.weather.service.WeatherGridConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherBatchService {

  private final RegionCoordinateConfig regionCoordinateConfig;
  private final WeatherGridConverter weatherGridConverter;
  private final WeatherCacheService weatherCacheService;

  /**
   * 초기는 서울과 경기 지역만 미리 캐시화 해두는 방식을 사용
   */
  private static final String[] PRELOAD_REGION_LIST = {"서울", "경기"};

  /**
   *
   */
  public void runWeatherPreloadBatch() {
    log.info("[배치] 날씨 정보 미리 로드 배치 실행 시작");
    long startTime = System.currentTimeMillis();
    int totalPreloaded = 0;
    int skipped = 0;
    Set<String> processedGridKeys = new HashSet<>();

    for (String sido : PRELOAD_REGION_LIST) {
      log.info("[배치] 지역별 날씨 정보 미리 로드 시작. sido={}", sido);
      Map<String, Coordinate> sigunguMap = regionCoordinateConfig.getRegionMap().get(sido);

      if (sigunguMap == null || sigunguMap.isEmpty()) {
        log.warn("지역 좌표 정보가 없습니다. sido={}", sido);
        skipped++;
        continue;
      }

      for (Map.Entry<String, Coordinate> sigunguEntry : sigunguMap.entrySet()) {
        String sigungu = sigunguEntry.getKey();
        Coordinate coordinate = sigunguEntry.getValue();

        WeatherGridPoint point = weatherGridConverter.convertToGrid(
                coordinate.getLat(),
                coordinate.getLng()
        );

        String gridKey = point.getNx() + ":" + point.getNy();
        if (!processedGridKeys.add(gridKey)) {
          log.info(
                  "중복 격자 스킵. sido={}, sigungu={}, nx={}, ny={}",
                  sido, sigungu, point.getNx(), point.getNy()
          );
          skipped++;
          continue;
        }
        weatherCacheService.getOrLoadWeather(point.getNx(), point.getNy());
        totalPreloaded++;
        log.info(
                "날씨 preload 완료. sido={}, sigungu={}, nx={}, ny={}",
                sido, sigungu, point.getNx(), point.getNy()
        );
      }
    }

    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;
    log.info("[배치] 날씨 정보 미리 로드 배치 실행 완료. 실행 시간: {}ms, 총 미리 로드된 날씨 정보 수: {}, 총 스킵된 격자 수: {}", executionTime, totalPreloaded, skipped);
  }
}
