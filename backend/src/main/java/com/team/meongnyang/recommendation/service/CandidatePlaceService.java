package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 가능한 장소 후보를 1차로 수집하고 위험한 조건을 먼저 걸러내는 후보 추출 계층이다.
 *
 * 파이프라인 흐름에서 날씨 조회 직후 호출되며,
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
  private static final String[] QUIET_INDOOR_KEYWORDS = {"실내", "카페", "조용", "휴식", "아늑", "편안", "라운지"};
  private static final String[] ACTIVE_OUTDOOR_KEYWORDS = {"공원", "산책로", "야외", "운동", "넓음", "잔디", "트레킹"};
  private static final String[] SMALL_FRIENDLY_KEYWORDS = {"실내", "카페", "아늑", "조용", "휴식"};

  private final PlaceRepository placeRepository;
  private final DistanceCalculator distanceCalculator;
  private final ActivityRadiusPolicy activityRadiusPolicy;

  /**
   * 사용자 위치와 현재 날씨를 기준으로 추천 가능한 1차 후보 장소 목록을 만든다.
   *
   * <p>검증된 장소, 좌표 유효성, 추천 가능 카테고리, 날씨 정책을 순서대로 적용하고,
   * 필요한 경우 날씨 조건을 완화하거나 핵심 조건만 남긴 fallback 후보를 반환한다.
   * 반경 조회는 사용자 activityRadius를 기준으로 수행하고, 후보가 없으면 확장 반경 단계로 재조회한다.
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

    // 2. 사용자 activityRadius를 정규화하고 fallback 확장 반경 단계를 계산
    ActivityRadiusPolicy.SearchRadiusPlan radiusPlan = activityRadiusPolicy.resolve(user);
    double maxDistanceKm = radiusPlan.appliedRadiusKm();

    // 3. 사용자 주변 반경 내 장소를 최대 BASE_FETCH_LIMIT개까지 조회
    List<Place> nearbyPlaces = fetchNearbyPlaces(lat, lng, radiusPlan, user, pet, weather);
    if (nearbyPlaces.isEmpty()) {
      log.warn("[장소 후보] 반경 내 후보 없음 userId={}, petId={}, batchExecutionId={}, userActivityRadius={}, appliedRadiusKm={}, walkLevel={}",
              user == null ? null : user.getUserId(),
              pet == null ? null : pet.getPetId(),
              RecommendationLogContext.batchExecutionId(),
              radiusPlan.requestedRadiusKm(),
              radiusPlan.appliedRadiusKm(),
              weather.getWalkLevel());
      return List.of();
    }

    // 4. 운영 여부, 좌표 유효성, 추천 카테고리, 날씨 조건을 기준으로 1차 필터 적용
    List<Place> strictCandidates = applyPrimaryFilters(nearbyPlaces, pet, weather, lat, lng, false);
    if (!strictCandidates.isEmpty()) {
      log.info("[장소 후보] 기본 조건 적용 userId={}, petId={}, batchExecutionId={}, userActivityRadius={}, appliedRadiusKm={}, count={}",
              user == null ? null : user.getUserId(),
              pet == null ? null : pet.getPetId(),
              RecommendationLogContext.batchExecutionId(),
              radiusPlan.requestedRadiusKm(),
              radiusPlan.appliedRadiusKm(),
              strictCandidates.size());
      return limitAndPrioritize(strictCandidates, pet);
    }

    // 4-2 날씨 조건 완화 필터 적용
    List<Place> relaxedCandidates = applyPrimaryFilters(nearbyPlaces, pet, weather, lat, lng, true);
    if (!relaxedCandidates.isEmpty()) {
      log.warn("[장소 후보] 날씨 조건 완화 userId={}, petId={}, batchExecutionId={}, userActivityRadius={}, appliedRadiusKm={}, count={}",
              user == null ? null : user.getUserId(),
              pet == null ? null : pet.getPetId(),
              RecommendationLogContext.batchExecutionId(),
              radiusPlan.requestedRadiusKm(),
              radiusPlan.appliedRadiusKm(),
              relaxedCandidates.size());
      return limitAndPrioritize(relaxedCandidates, pet);
    }

    // 4-3 엄격+완화 필터 반환이 없으면 fallback 필터 적용
    List<Place> fallbackCandidates = applyCoreFallbackFilters(nearbyPlaces, lat, lng, maxDistanceKm);
    if (!fallbackCandidates.isEmpty()) {
      log.warn("[장소 후보] 핵심 조건 fallback 적용 userId={}, petId={}, batchExecutionId={}, userActivityRadius={}, appliedRadiusKm={}, count={}",
              user == null ? null : user.getUserId(),
              pet == null ? null : pet.getPetId(),
              RecommendationLogContext.batchExecutionId(),
              radiusPlan.requestedRadiusKm(),
              radiusPlan.appliedRadiusKm(),
              fallbackCandidates.size());
      return limitAndPrioritize(fallbackCandidates, pet);
    }

    // 4-4 최종 후보가 없으면 빈 리스트 반환
    log.warn("[장소 후보] 최종 후보 없음 userId={}, petId={}, batchExecutionId={}, userActivityRadius={}, appliedRadiusKm={}, walkLevel={}",
            user == null ? null : user.getUserId(),
            pet == null ? null : pet.getPetId(),
            RecommendationLogContext.batchExecutionId(),
            radiusPlan.requestedRadiusKm(),
            radiusPlan.appliedRadiusKm(),
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
   * 사용자 activityRadius 기반 반경 정책으로 주변 장소를 가져온다.
   * @param lat 좌표
   * @param lng 좌표
   * @param radiusPlan 반경 정책 결과
   * @param user 사용자 정보
   * @param pet 반려동물 정보
   * @param weather 날씨 정보
   * @return 주변 장소 목록
   */
  private List<Place> fetchNearbyPlaces(
          double lat,
          double lng,
          ActivityRadiusPolicy.SearchRadiusPlan radiusPlan,
          User user,
          Pet pet,
          WeatherContext weather
  ) {
    List<Place> nearbyPlaces = List.of();

    for (Integer radiusKm : radiusPlan.fallbackRadiusStepsKm()) {
      int radiusMeters = activityRadiusPolicy.toMeters(radiusKm);
      nearbyPlaces = placeRepository.findNearby(lat, lng, radiusMeters, BASE_FETCH_LIMIT);

      log.info("[장소 후보] 반경 조회 userId={}, petId={}, batchExecutionId={}, userActivityRadius={}, appliedRadiusKm={}, queryRadiusKm={}, queryRadiusMeters={}, count={}, walkLevel={}",
              user == null ? null : user.getUserId(),
              pet == null ? null : pet.getPetId(),
              RecommendationLogContext.batchExecutionId(),
              radiusPlan.requestedRadiusKm(),
              radiusPlan.appliedRadiusKm(),
              radiusKm,
              radiusMeters,
              nearbyPlaces.size(),
              weather.getWalkLevel());

      if (!nearbyPlaces.isEmpty()) {
        return nearbyPlaces;
      }
    }

    return nearbyPlaces;
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
      // 날씨 조건에 맞는지 확인
      if (!matchesWeatherPolicy(place, weather, relaxWeather)) {
        continue;
      }

      result.add(place);
    }

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
            .sorted(Comparator.<Place>comparingInt(place -> personalizationPriorityScore(place, pet)).reversed())
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
      if (containsKeyword(place, preferredPlace) || matchesPreferredPlace(place, preferredPlace)) {
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

    log.info("[장소 후보] 선호 장소 우선순위 적용 userId={}, petId={}, batchExecutionId={}, preferredCount={}, normalCount={}",
            RecommendationLogContext.userId(),
            RecommendationLogContext.petId(),
            RecommendationLogContext.batchExecutionId(),
            preferredMatches.size(),
            normalMatches.size());
    return prioritized;
  }

  private int personalizationPriorityScore(Place place, Pet pet) {
    if (pet == null) {
      return 0;
    }

    int score = 0;
    String preferredPlace = RecommendationTextUtils.normalizeTrimLower(pet.getPreferredPlace());
    String joined = buildSearchableText(place);

    if (!preferredPlace.isBlank()) {
      if (containsKeyword(place, preferredPlace)) {
        score += 6;
      } else if (matchesPreferredPlace(place, preferredPlace)) {
        score += 4;
      }
    }

    if (pet.getPetActivity() == Pet.PetActivity.HIGH && containsAny(joined, ACTIVE_OUTDOOR_KEYWORDS)) {
      score += 3;
    }
    if (pet.getPetActivity() == Pet.PetActivity.LOW && containsAny(joined, QUIET_INDOOR_KEYWORDS)) {
      score += 3;
    }
    if (pet.getPetSize() == Pet.PetSize.LARGE && containsAny(joined, ACTIVE_OUTDOOR_KEYWORDS)) {
      score += 2;
    }
    if (pet.getPetSize() == Pet.PetSize.SMALL && containsAny(joined, SMALL_FRIENDLY_KEYWORDS)) {
      score += 2;
    }

    String personality = RecommendationTextUtils.normalizeTrimLower(pet.getPersonality());
    if (!personality.isBlank()) {
      if (containsAny(personality, "예민", "조용", "겁") && containsAny(joined, QUIET_INDOOR_KEYWORDS)) {
        score += 2;
      }
      if (containsAny(personality, "사교", "활발") && containsAny(joined, "카페", "공원", "넓음", "산책로")) {
        score += 1;
      }
    }

    return score;
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
    String joined = buildSearchableText(place);
    return joined.contains(keyword);
  }

  private String buildSearchableText(Place place) {
    return List.of(
                    RecommendationTextUtils.normalizeTrimLower(place.getCategory()),
                    RecommendationTextUtils.normalizeTrimLower(place.getTitle()),
                    RecommendationTextUtils.normalizeTrimLower(place.getDescription()),
                    RecommendationTextUtils.normalizeTrimLower(place.getOverview()),
                    RecommendationTextUtils.normalizeTrimLower(place.getTags()),
                    RecommendationTextUtils.normalizeTrimLower(place.getBlogPositiveTags())
            ).stream()
            .collect(Collectors.joining(" "));
  }

  private boolean matchesPreferredPlace(Place place, String preferredPlace) {
    String joined = buildSearchableText(place);
    if (preferredPlace.contains("실내카페")) {
      return "DINING".equalsIgnoreCase(place.getCategory())
              && containsAny(joined, QUIET_INDOOR_KEYWORDS);
    }
    if (preferredPlace.contains("공원")) {
      return "PLACE".equalsIgnoreCase(place.getCategory())
              && containsAny(joined, ACTIVE_OUTDOOR_KEYWORDS);
    }
    if (preferredPlace.contains("야외")) {
      return containsAny(joined, ACTIVE_OUTDOOR_KEYWORDS);
    }
    return false;
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private boolean isCoordinateValue(double lat, double lng) {
    return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
  }
}
