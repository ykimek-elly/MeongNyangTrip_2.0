package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 가능한 장소 후보를 1차로 수집하고 위험한 조건을 먼저 걸러내는 후보 추출 계층이다.
 *
 * <p>파이프라인 흐름에서 날씨 조회 직후 호출되며,
 * 사용자 좌표와 날씨, 반려동물 선호를 기준으로 추천에 사용할 후보 장소 집합을 만든다.
 * 여기서 반환한 결과는 이후 {@link PlaceScoringService}의 점수 계산 입력으로 사용된다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CandidatePlaceService {

  /** 추천 가능한 카테고리 목록 */
  private static final Set<String> RECOMMENDABLE_CATEGORIES = Set.of("PLACE", "DINING", "STAY");
  /** 기본 후보 목록의 최대 크기 */
  private static final int BASE_FETCH_LIMIT = 120;
  /** 최종 후보 목록의 최대 크기 */
  private static final int RESULT_LIMIT = 20;
  /** Walk Level == Good 일때 최대 거리 */
  private static final double GOOD_MAX_DISTANCE_KM = 12.0;
  /** Walk Level == Caution 일때 최대 거리 */
  private static final double CAUTION_MAX_DISTANCE_KM = 8.0;
  /** Walk Level == Dangerous 일때 최대 거리 */
  private static final double DANGEROUS_MAX_DISTANCE_KM = 5.0;


  private final PlaceRepository placeRepository;
  private final DistanceCalculator distanceCalculator;

  /**
   * 사용자 위치와 현재 날씨를 기준으로 추천 가능한 1차 후보 장소 목록을 만든다.
   *
   * <p>검증된 장소, 좌표 유효성, 추천 가능 카테고리, 거리 제한, 날씨 정책을 순서대로 적용하고,
   * 필요한 경우 날씨 조건을 완화하거나 핵심 조건만 남긴 fallback 후보를 반환한다.
   *
   * @param user 추천 대상 사용자 정보
   * @param pet 후보 우선순위에 반영할 반려동물 정보
   * @param weather 현재 추천에 반영할 날씨 문맥
   * @param lat 사용자 위도
   * @param lng 사용자 경도
   * @return 점수 계산 단계로 전달할 1차 후보 장소 목록
   */
  public List<Place> getInitialCandidates(
          User user,
          Pet pet,
          WeatherContext weather,
          double lat,
          double lng
  ) {
    // 1. 입력값 유효성 검사
    validateInput(weather, lat, lng);

    // 2. walkLevel을 정규화한 뒤 산책 등급별 최대 반경을 계산하고 미터 단위로 변환
    String walkLevel = RecommendationTextUtils.normalizeTrimLower(weather.getWalkLevel()); // WalkLevel
    double maxDistanceKm = resolveMaxDistanceKm(walkLevel); // 최대거리 기준
    int radiusMeters = toMeters(maxDistanceKm); // 미터로 변환

    // 3. 사용자 주변 반경 내 장소를 최대 BASE_FETCH_LIMIT개까지 조회
    // 최대 120개
    List<Place> nearbyPlaces = fetchNearbyPlaces(lat, lng, radiusMeters);
    // 비어있으면 빈 리스트 반환
    if (nearbyPlaces.isEmpty()) {
      log.info("[장소 필터] 반경 내 후보 장소가 없습니다. 반경(m)={}", radiusMeters);
      return List.of();
    }


    // 4. 운영 여부, 좌표 유효성, 추천 카테고리, 거리 제한, 날씨 조건을 기준으로 1차 필터 적용
        // 선호 장소 일치 후보를 먼저 배치하여 약한 우선순위를 부여

    // 4-1. 날씨 조건을 포함한 엄격 필터 적용
    List<Place> strictCandidates = applyPrimaryFilters(nearbyPlaces, pet, weather, lat, lng, false);
    // 엄격 필터 결과가 있으면 대표견 선호 장소를 약하게 우선 반영한 뒤 RESULT_LIMIT개까지 반환
    if (!strictCandidates.isEmpty()) {
      log.info("[장소 필터] 엄격 필터를 적용했습니다. 후보 수={}", strictCandidates.size());
      return limitAndPrioritize(strictCandidates, pet);
    }

    // 4-2 날씨 조건 완화 필터 적용
    List<Place> relaxedCandidates = applyPrimaryFilters(nearbyPlaces, pet, weather, lat, lng, true);
      // 완화 필터를 거쳐 반환된 후보가 있다면 펫의 선호 장소가 있는지 확인 후
      // 최대 RESULT_LIMIT 개를 반환합니다
    if (!relaxedCandidates.isEmpty()) {
      log.info("[장소 필터] 엄격 필터 결과가 없어 날씨 조건을 완화한 후보를 반환합니다.");
      return limitAndPrioritize(relaxedCandidates, pet);
    }

    // 4-3 엄격+완화 필터 반환이 없으면 fallback 필터 적용
    List<Place> fallbackCandidates = applyCoreFallbackFilters(nearbyPlaces, lat, lng, maxDistanceKm);
    if (!fallbackCandidates.isEmpty()) {
      log.info("[장소 필터] 날씨 필터 fallback을 적용했습니다. 후보 수={}", fallbackCandidates.size());
      return limitAndPrioritize(fallbackCandidates, pet);
    }

    // 4-4 최종 후보가 없으면 빈 리스트 반환
    log.info("[장소 필터] 최종 후보가 없습니다. userId={}, 산책 등급={}",
            user == null ? null : user.getUserId(),
            weather.getWalkLevel());
    return List.of();
  }

  /**
   * 입력값 유효성 검사
   * @param weather 날씨 정보
   * @param lat 좌표
   * @param lng 좌표
   */
  private void validateInput(WeatherContext weather, double lat, double lng) {
    if (weather == null) {
      throw new IllegalArgumentException("weather must not be null");
    }
    if (!isCoordinateValue(lat, lng)) {
      throw new IllegalArgumentException("user coordinates are invalid");
    }
  }

  /**
   * 장소를 주변에서 가져옴
   * @param lat 좌표
   * @param lng 좌표
   * @param radiusMeters 반경(m)
   * @return 주변 장소 목록
   */
  private List<Place> fetchNearbyPlaces(double lat, double lng, int radiusMeters) {
    // radiusMeters (m) 안 최대 BASE_FETCH_LIMIT 개 반환
    return placeRepository.findNearby(lat, lng, radiusMeters, BASE_FETCH_LIMIT);
  }

  /**
   * 주변 장소에서 필터링을 적용하여 추천 가능한 장소를 찾음
   * @param places 주변 장소 목록
   * @param pet 사용자 반려동물 정보
   * @param weather 날씨 정보
   * @param userLat 사용자 좌표
   * @param userLng 사용자 좌표
   * @param relaxWeather 날씨 조건을 느슨하게 적용할지 여부
   * @return 필터링된 추천 가능한 장소 목록
   */
  private List<Place> applyPrimaryFilters(
          List<Place> places,
          Pet pet,
          WeatherContext weather,
          double userLat,
          double userLng,
          boolean relaxWeather
  ) {
    String walkLevel = RecommendationTextUtils.normalizeTrimLower(weather.getWalkLevel());
    double maxDistanceKm = resolveMaxDistanceKm(walkLevel);
    List<Place> result = new ArrayList<>();

    for (Place place : places) {
      // 운영 여부
      if (!isOperationalPlace(place)) {
        continue;
      }
      // 좌표 유효성
      if (!hasCoordinates(place)) {
        continue;
      }
      // 장소 카테고리 추천 가능 여부
      if (!isRecommendableCategory(place)) {
        continue;
      }
      // 거리 제한
      if (!isWithinDistance(place, userLat, userLng, maxDistanceKm)) {
        continue;
      }
      // 날씨 조건에 맞는지 확인
      if (!matchesWeatherPolicy(place, weather, relaxWeather)) {
        continue;
      }

      result.add(place);
    }

    log.info("[장소 필터] 1차 필터 결과입니다. 날씨 완화 적용={}, 후보 수={}", relaxWeather, result.size());
    return result;
  }

  /**
   * FallBack Fillter
   * - 필터링된 후보 목록에서 최종 후보를 선택합니다.
   * @param places 후보 목록
   * @param userLat 사용자의 위도
   * @param userLng 사용자의 경도
   * @param maxDistanceKm 최대 거리 제한 (km)
   * @return 최종 후보 목록
   */
  private List<Place> applyCoreFallbackFilters(
          List<Place> places,
          double userLat,
          double userLng,
          double maxDistanceKm
  ) {
    /**
     * 필터링된 후보 목록에서 최종 후보를 선택합니다.
     * - 운영 여부 + 좌표 유효성 + 장소 카테고리 추천 가능 여부/ 거리 제한
     */
    return places.stream()
            .filter(this::isOperationalPlace) // 운영여부
            .filter(this::hasCoordinates) // 좌표 유효성
            .filter(this::isRecommendableCategory) // 장소 카테고리 추천 가능 여부
            .filter(place -> isWithinDistance(place, userLat, userLng, maxDistanceKm)) // 거리 제한
            .limit(RESULT_LIMIT) // 최대 결과 제한
            .toList();
  }

  /**
   * 후보 목록을 최종 결과로 제한하고 우선순위를 부여합니다.
   * @param candidates 후보 목록
   * @param pet 사용자의 반려동물 정보
   * @return 최종 후보 목록
   */
  private List<Place> limitAndPrioritize(List<Place> candidates, Pet pet) {
    List<Place> prioritized = weaklyPrioritizePreferredPlace(candidates, pet);
    return prioritized.stream()
            .limit(RESULT_LIMIT)
            .toList();
  }

  /**
   * 후보 목록에서 사용자의 선호 장소를 우선적으로 선택합니다.
   * @param candidates 후보 목록
   * @param pet 사용자의 반려동물 정보
   * @return 우선순위가 높은 후보 목록
   */
  private List<Place> weaklyPrioritizePreferredPlace(List<Place> candidates, Pet pet) {
    String preferredPlace = RecommendationTextUtils.normalizeTrimLower(pet == null ? null : pet.getPreferredPlace());
    if (preferredPlace.isBlank()) {
      return candidates;
    }

    List<Place> preferredMatches = new ArrayList<>();
    List<Place> normalMatches = new ArrayList<>();

    for (Place place : candidates) {
      if (containsKeyword(place, preferredPlace)) {
        // 선호 장소가 일치하는 경우 우선순위를 높임
        preferredMatches.add(place);
      } else {
        // 선호 장소가 일치하지 않는 경우 일반적인 후보로 분류
        normalMatches.add(place);
      }
    }

    List<Place> prioritized = new ArrayList<>(preferredMatches.size() + normalMatches.size());
    // 선호하는 장소를 앞에 배치
    prioritized.addAll(preferredMatches);
    prioritized.addAll(normalMatches);

    log.info("[장소 필터] 대표견 선호 장소 약한 우선순위를 적용했습니다. 선호 일치={}, 일반={}",
            preferredMatches.size(),
            normalMatches.size());
    return prioritized;
  }

  /**
   * 장소가 운영 중인지 확인
   * @param place 장소 정보
   * @return 운영 중인 경우 true, 그렇지 않은 경우 false
   */
  private boolean isOperationalPlace(Place place) {
    if (place == null) {
      return false;
    }
    if (!Boolean.TRUE.equals(place.getIsVerified())) {
      log.debug("[PLACE_FILTER][DROP] not verified. title={}", place.getTitle());
      return false;
    }

    String tags = RecommendationTextUtils.normalizeTrimLower(place.getTags());
    if (tags.contains("폐업") || tags.contains("휴업") || tags.contains("운영종료")) {
      log.debug("[PLACE_FILTER][DROP] closed tag detected. title={}", place.getTitle());
      return false;
    }

    return true;
  }

  /**
   * 장소 좌표가 유효한지 확인
   * @param place 장소 정보
   * @return 좌표가 유효한 경우 true, 그렇지 않은 경우 false
   */
  private boolean hasCoordinates(Place place) {
    boolean result = place.getLatitude() != null
            && place.getLongitude() != null
            && isCoordinateValue(place.getLatitude(), place.getLongitude());

    if (!result) {
      log.debug("[PLACE_FILTER][DROP] invalid coordinates. title={}", place.getTitle());
    }
    return result;
  }

  /**
   * 장소 카테고리가 추천 가능한지 확인
   * @param place 장소 정보
   * @return 추천 가능한 경우 true, 그렇지 않은 경우 false
   */
  private boolean isRecommendableCategory(Place place) {
    String category = RecommendationTextUtils.normalizeTrimLower(place.getCategory()).toUpperCase(Locale.ROOT);
    boolean result = RECOMMENDABLE_CATEGORIES.contains(category);

    if (!result) {
      log.debug("[PLACE_FILTER][DROP] unsupported category. title={}, category={}", place.getTitle(), place.getCategory());
    }
    return result;
  }

  /**
   * 장소가 사용자로부터의 거리 내에 있는지 확인
   * @param place 장소 정보
   * @param userLat 사용자 위도
   * @param userLng 사용자 경도
   * @param maxDistanceKm 최대 거리 (km)
   * @return 거리 내에 있는 경우 true, 그렇지 않은 경우 false
   */
  private boolean isWithinDistance(Place place, double userLat, double userLng, double maxDistanceKm) {
    double distanceKm = distanceCalculator.calculateDistanceKm(
            userLat,
            userLng,
            place.getLatitude(),
            place.getLongitude()
    );

    boolean result = distanceKm <= maxDistanceKm;
    if (!result) {
      log.debug("[PLACE_FILTER][DROP] too far. title={}, distanceKm={}, maxDistanceKm={}",
              place.getTitle(),
              distanceKm,
              maxDistanceKm);
    }
    return result;
  }

  /**
   * 장소가 날씨 정책에 맞는지 확인
   * @param place 장소 정보
   * @param weather 날씨 정보
   * @param relaxWeather 날씨 정책을 무시할지 여부
   * @return 정책에 맞는 경우 true, 그렇지 않은 경우 false
   */
  private boolean matchesWeatherPolicy(Place place, WeatherContext weather, boolean relaxWeather) {
    String walkLevel = RecommendationTextUtils.normalizeTrimLower(weather.getWalkLevel());

    if (walkLevel.isBlank()) {
      return true;
    }
    if (relaxWeather) {
      return true;
    }

    boolean indoor = isIndoorPlace(place);
    boolean outdoor = isOutdoorPlace(place);

    if ("dangerous".equals(walkLevel)) {
      return indoor;
    }
    if ("caution".equals(walkLevel)) {
      return indoor || !outdoor;
    }
    return !weather.isRaining() || indoor || !outdoor;
  }

  /**
   * 장소가 실내 장소인지 확인
   * @param place 장소 정보
   * @return 실내 장소인 경우 true, 그렇지 않은 경우 false
   */
  private boolean isIndoorPlace(Place place) {
    String normalized = RecommendationTextUtils.normalizeTrimLower(place.getTags());
    return normalized.contains("실내")
            || normalized.contains("카페")
            || normalized.contains("박물관")
            || normalized.contains("전시")
            || ("DINING".equalsIgnoreCase(place.getCategory()) && !normalized.contains("야외"));
  }

  /**
   * 장소가 실외 장소인지 확인
   * @param place 장소 정보
   * @return 실외 장소인 경우 true, 그렇지 않은 경우 false
   */
  private boolean isOutdoorPlace(Place place) {
    String normalized = RecommendationTextUtils.normalizeTrimLower(place.getTags());
    return normalized.contains("실외")
            || normalized.contains("야외")
            || normalized.contains("공원")
            || normalized.contains("산책로")
            || normalized.contains("운동장");
  }

  /**
   * 장소에 특정 키워드가 포함되어 있는지 확인
   * @param place 장소 정보
   * @param keyword 검색 키워드
   * @return 키워드가 포함된 경우 true, 그렇지 않은 경우 false
   */
  private boolean containsKeyword(Place place, String keyword) {
    String joined = List.of(
                    RecommendationTextUtils.normalizeTrimLower(place.getCategory()),
                    RecommendationTextUtils.normalizeTrimLower(place.getTitle()),
                    RecommendationTextUtils.normalizeTrimLower(place.getDescription()),
                    RecommendationTextUtils.normalizeTrimLower(place.getTags())
            ).stream()
            .collect(Collectors.joining(" "));
    return joined.contains(keyword);
  }

  /**
   * 걷기 수준에 따른 최대 거리 계산
   * @param walkLevel 걷기 수준
   * @return 최대 거리 (km)
   */
  private double  resolveMaxDistanceKm(String walkLevel) {
    if ("dangerous".equals(walkLevel)) {
      return DANGEROUS_MAX_DISTANCE_KM;
    }
    if ("caution".equals(walkLevel)) {
      return CAUTION_MAX_DISTANCE_KM;
    }
    return GOOD_MAX_DISTANCE_KM;
  }

  /**
   * 거리(km)를 미터(m)로 변환
   * @param distanceKm 거리(km)
   * @return 거리(m)
   */
  private int toMeters(double distanceKm) {
    return (int) Math.round(distanceKm * 1000);
  }

  private boolean isCoordinateValue(double lat, double lng) {
    return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
  }

}
