package com.team.meongnyang.recommendation.util;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.recommendation.dto.Coordinate;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * 서울 / 경기 지역의 시군구별 대표 위경도 좌표를 관리하는 설정 클래스
 *
 * <p>JSON 리소스 파일을 애플리케이션 시작 시 1회 로딩하여
 * 시도 -> 시군구 -> Coordinate 구조로 메모리에 보관한다.</p>
 */
@Getter
@Component
@RequiredArgsConstructor
public class RegionCoordinateConfig {

  private static final String RESOURCE_PATH = "region-coordinates.json";

  private final ObjectMapper objectMapper;

  private Map<String, Map<String, Coordinate>> regionMap;

  @PostConstruct
  public void init() {
    try (InputStream inputStream = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
      Map<String, Map<String, Coordinate>> loaded =
              objectMapper.readValue(inputStream, new TypeReference<>() {});

      this.regionMap = Collections.unmodifiableMap(loaded);
    } catch (Exception e) {
      throw new IllegalStateException("지역 좌표 JSON 로딩에 실패했습니다. path=" + RESOURCE_PATH, e);
    }
  }

  /**
   * 시도 / 시군구에 해당하는 대표 좌표를 반환한다.
   *
   * @param sido 시도명 (예: 서울, 경기)
   * @param sigungu 시군구명 (예: 강남구, 수원시)
   * @return 대표 좌표, 없으면 null
   */
  public Coordinate get(String sido, String sigungu) {
    Map<String, Coordinate> sigunguMap = regionMap.get(sido);
    if (sigunguMap == null) {
      return null;
    }
    return sigunguMap.get(sigungu);
  }
}