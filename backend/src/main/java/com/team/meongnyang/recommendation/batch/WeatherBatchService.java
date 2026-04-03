package com.team.meongnyang.recommendation.batch;

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

/**
 * 날씨 데이터를 사전에 Redis 캐시에 적재하는 배치 서비스
 *
 * 추천 파이프라인에서 날씨 API 호출은 빈번하게 발생하기 때문에,
 * 주요 지역(서울/경기)의 날씨를 미리 조회하여 캐시에 저장함으로써
 * 실시간 추천 요청 시 API 호출 비용과 latency를 줄이는 것을 목적으로 한다.
 *
 * 동작 방식
 *   사전 정의된 지역 목록(PRELOAD_REGION_LIST)을 순회
 *   시군구 좌표(lat, lng)를 KMA 격자(nx, ny)로 변환
 *   중복 격자 제거 후 캐시 적재
 *
 * 특징
 *   동일 격자(nx, ny)에 대한 중복 호출 방지
 *   WeatherCacheService를 통해 캐시 HIT/MISS 관리
 *   배치 실행 시간 및 처리 통계 로깅
 *
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherBatchService {

  private final RegionCoordinateConfig regionCoordinateConfig;
  private final WeatherGridConverter weatherGridConverter;
  private final WeatherCacheService weatherCacheService;


  private static final String[] PRELOAD_REGION_LIST = {"서울", "경기"};

  /**
   * 서울/경기 주요 지역의 날씨 정보를 Redis 캐시에 미리 적재하는 배치 실행 메서드.
   *
   * 각 지역의 시군구 좌표를 순회하며 날씨 API를 호출하고,
   * 결과를 캐시에 저장한다. 이미 처리된 격자(nx, ny)는 중복 호출을 방지하기 위해 스킵된다.
   *
   * 처리 흐름
   *   PRELOAD_REGION_LIST 기준으로 시도 순회
   *   시군구 좌표 조회
   *   위경도 → KMA 격자(nx, ny) 변환
   *   중복 격자 체크
   *   WeatherCacheService를 통해 캐시 적재
   *
   * 로깅 정보
   *   배치 시작/종료 시간
   *   preload 성공 개수
   *   중복 또는 예외로 인한 스킵 개수
   */
  public void runWeatherPreloadBatch() {
    log.info("[날씨 조회] 선적재 배치 시작");
    // 배치 시작 로그 및 실행 시간 측정 시작
    long startTime = System.currentTimeMillis();
    // preload 성공 개수
    int totalPreloaded = 0;
    // 중복 또는 좌표 없음으로 스킵된 개수
    int skipped = 0;
    // 이미 처리한 격자(nx:ny)를 저장하여 중복 호출 방지
    Set<String> processedGridKeys = new HashSet<>();

    // 시도(서울, 경기) 단위로 순회
    for (String sido : PRELOAD_REGION_LIST) {

      // 시군구 좌표 목록 조회
      Map<String, Coordinate> sigunguMap = regionCoordinateConfig.getRegionMap().get(sido);

      // 좌표 정보가 없으면 스킵
      if (sigunguMap == null || sigunguMap.isEmpty()) {
        log.warn("[날씨 조회] 지역 좌표 없음 sido={}", sido);
        skipped++;
        continue;
      }

      for (Map.Entry<String, Coordinate> sigunguEntry : sigunguMap.entrySet()) {
        String sigungu = sigunguEntry.getKey();
        Coordinate coordinate = sigunguEntry.getValue();

        // 위경도 → KMA 격자 좌표(nx, ny) 변환
        WeatherGridPoint point = weatherGridConverter.convertToGrid(
                coordinate.getLat(),
                coordinate.getLng()
        );

        // nx:ny 형태의 유니크 키 생성 (같은 격자 중복 제거용)
        String gridKey = point.getNx() + ":" + point.getNy();
        // 이미 처리된 격자라면 스킵
        if (!processedGridKeys.add(gridKey)) {
          skipped++;
          continue;
        }
        // 캐시 조회 또는 API 호출 후 캐시에 적재
        weatherCacheService.getOrLoadWeather(point.getNx(), point.getNy());
        // preload 성공 카운트 증가
        totalPreloaded++;
      }
    }

    // 배치 종료 시간 계산
    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;
    // 최종 통계 로그
    log.info("[날씨 조회] 선적재 배치 종료 elapsedMs={}, loadedCount={}, skippedCount={}",
            executionTime, totalPreloaded, skipped);
  }
}
