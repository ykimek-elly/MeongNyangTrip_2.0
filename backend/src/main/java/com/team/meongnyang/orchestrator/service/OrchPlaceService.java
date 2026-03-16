package com.team.meongnyang.orchestrator.service;

import com.team.meongnyang.weather.dto.WeatherContext;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 장소 추천용 1차 후보 조회 서비스
 *
 * 역할:
 * - 날씨 상태에 따라 장소 조회 전략을 나눈다.
 * - 조회된 후보군에 대해 1차 필터를 적용한다.
 * - 아직 점수 계산은 하지 않고, 점수 계산 대상이 될 후보군만 반환한다.
 *
 * 1차 필터 기준 :
 * -  운영 중인 장소만 포함
 * -  좌표가 있는 장소만 포함
 * -  사용자와 너무 먼 장소 제외
 * -  날씨에 따라 실내/실외 조건 분기
 * -  추천 대상 카테고리만 포함
 * -  필요하면 대표견 선호 장소 정도만 약하게 반
 */

@Service("orchestratorPlaceService")
@Slf4j
@RequiredArgsConstructor
public class OrchPlaceService {
  private final PlaceRepository placeRepository;
  private final CalcDistanceService calcDistanceService;

  /**
   * 추천용 초기 후보 장소를 조회한다.
   *
   * 처리 흐름:
   * 1. 날씨에 따라 조회 전략을 정한다.
   * 2. DB에서 후보 장소를 조회한다.
   * 3. 좌표 유효성, 실내 여부 등의 1차 필터를 적용한다.
   * 4. 점수 계산 서비스로 넘길 후보군을 반환한다.
   *
   * @param user 사용자 정보
   * @param dog 반려견 정보
   * @param weather 현재 날씨 정보
   * @return 1차 필터를 통과한 장소 후보 리스트
   */
  public List<Place> getInitialCandidates(User user, Dog dog, WeatherContext weather ,
                                          double lat, double lng) {

    if (weather == null) {
      throw new IllegalArgumentException("weather 는 null 일 수 없습니다.");
    }

    // 1. 날씨에 따라 조회 전략 변경
    String walkLevel = weather.getWalkLevel();
    int fetchLimit = 30;
    int resultLimit = 5;
    Pageable pageable = PageRequest.of(0, fetchLimit);



    // 날씨가 위험 단계면 장소 추천 대신 빈 리스트 반환
    if("DANGEROUS".equalsIgnoreCase(walkLevel)){
      log.info("산책 위험 단계 - 장소 후보 대신 빈 리스트가 반환되었습니다.");
      return Collections.emptyList();
    }

    // 1. 날씨에 따라 조회 전략 분기
    List<Place> rawCandidates;

    // 주의 혹은 API 호출 실패시
    if("CAUTION".equalsIgnoreCase(walkLevel) || "ERROR".equalsIgnoreCase(walkLevel)) {
      // 주의단계면 실내 태그가 포함된 장소를 우선 대상으로 조회
      rawCandidates = placeRepository.findByTagsContaining("실내", pageable);
      log.info("CAUTION 날씨 - 실내 후보 조회");
    } else {
      // 날씨가 좋으면 전체 후보 조회
      rawCandidates = placeRepository.findAll(pageable).getContent();
      log.info("GOOD 날씨 - 전체 후보 조회");
    }

    // 조회 결과가 없으면 빈 리스트 반환
    if (rawCandidates == null || rawCandidates.isEmpty()) {
      log.info("장소 조회 결과가 없어 빈 리스트가 반환되었습니다. DB를 확인해주세요,");
      return Collections.emptyList();
    }

    // 2. 1차 필터 적용
    List<Place> results = new ArrayList<>();

    for (Place place : rawCandidates) {
      // 2-1 좌표가 없는 장소는 제외
      if (place.getLatitude() == null || place.getLongitude() == null) {
        log.info("[탈락] 좌표가 없는 장소는 제외합니다 - {}", place.getTitle());
        continue;
      }

      // 2-2 사용자 좌표와 장소 좌표 거리 계산
      double distanceKm = calcDistanceService.calculateDistanceKm(
              lat,
              lng,
              place.getLatitude(),
              place.getLongitude()
      );

      log.info("후보 체크: 장소명={}, userLat={}, userLng={}, placeLat={}, placeLng={}, distance={}km",
              place.getTitle(),
              lat, lng,
              place.getLatitude(), place.getLongitude(),
              distanceKm);

      // 직선거리 10km 이상 차이나면 제외
      if (distanceKm > 15.0) {
        log.info("[탈락] {} - 거리 초과", place.getTitle());
        continue;
      }

      // 2-3 날씨가 CAUTION 인 경우 실내만 허용
      if ("CAUTION".equalsIgnoreCase(walkLevel) && !isIndoorPlace(place)) {
        log.info("[탈락] {} - 실내 장소 아님", place.getTitle());
        continue;
      }

      results.add(place);
      log.info("[통과] {}", place.getTitle());

      // 최종 후보 개수 제한
      if (results.size() >= resultLimit) {
        break;
      }

    }
    log.info("1차 필터 통과 후보 수 : {} ", results.size());
    return results;
  }

  // 장소 태그에 "실내"가 포홤되어있는지 확인
  private boolean isIndoorPlace (Place place) {
    return place.getTags() != null && place.getTags().contains("실내");
  }
  // 장소 태그에 "실외"가 포홤되어있는지 확인
  private boolean isOutdoorPlace (Place place) {
    return place.getTags() != null && place.getTags().contains("실외");
  }

}
