package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.rag.service.RagService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherGridPoint;
import com.team.meongnyang.recommendation.cache.WeatherCacheService;
import com.team.meongnyang.recommendation.weather.service.WeatherGridConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 반려동물 동반 장소 추천 흐름 전체를 조합하는 파이프라인 진입점이다.
 *
 * <p>사용자와 반려동물 정보 조회, 날씨 조회, 후보 장소 수집, 점수 계산, RAG 문맥 조회,
 * 프롬프트 생성, Gemini 응답 생성과 캐시 확인, AI 응답 로그 저장까지 추천 파이프라인의 핵심 단계를
 * 순서대로 연결한다.
 *
 * <p>이 클래스에서 만든 최종 추천 문장은 추천 API 응답으로 바로 반환되며,
 * 동일한 입력에 대해서는 캐시 재사용과 로그 분석에도 활용된다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RecommendationPipelineService {
  private static final int LOG_TOP_PLACE_LIMIT = 3;

  private final RecommendationUserReader recommendationUserReader;
  private final RecommnedationPetReader recommnedationPetReader;
  private final WeatherCacheService weatherService;
  private final CandidatePlaceService candidatePlaceService;
  private final RagService ragService;
  private final PlaceScoringService placeScoringService;
  private final RecommendationPromptService recommendationPromptService;
  private final GeminiCacheService geminiCacheService;
  private final GeminiRecommendationService geminiRecommendationService;
  private final WeatherGridConverter weatherGridConverter;
  private final AiLogService aiLogservice;

  private static final double SUWON_LAT = 37.27;
  private static final double SUWON_LNG = 127.01;

  public RecommendationNotificationResult recommendForCurrentUser(String email) {
    // 1. 현재 로그인 사용자를 조회한다.
    User user = recommendationUserReader.getCurrentUserByEmail(email);
    log.info("[추천 파이프라인] 사용자 조회 결과 email={}, userId={}, nickname={}", email, user.getUserId(), user.getNickname());
    log.info("[추천 파이프라인] 사용자 위경도 lat={}, lng={}", SUWON_LAT, SUWON_LNG);
    // todo : 사용자로부터 위치를 받는지, 위치 데이터가 따로 넘어오는지, 현재는 임의로 정해둔 SUWON LAT,LNG

    // 2. 대표 반려견 정보를 조회한다.
    Pet pet = recommnedationPetReader.getPrimaryPet(user);
    log.info("[추천 파이프라인] 대표 반려견 조회 결과 petId={}, name={}, preferredPlace={}", pet.getPetId(), pet.getPetName(), pet.getPreferredPlace());

    // 3. 공통 추천 로직 실행
    return recommendForNotification(user, pet);
  }

  /**
   * 배치/알림 전송용 추천 문장을 생성한다.
   *
   * <p>이미 조회된 사용자와 반려견 정보를 받아
   * 현재 날씨 조회, 후보 장소 수집, 점수 계산, 프롬프트 생성,
   * Gemini 응답 생성 또는 캐시 재사용, AI 로그 저장까지 수행한다.
   *
   * @param user 추천 대상 사용자
   * @param pet 추천 대상 반려견
   * @return 사용자에게 전달할 추천 문장 또는 fallback 메시지
   */
  public RecommendationNotificationResult recommendForNotification(User user, Pet pet) {
    log.info("[추천 파이프라인-알림] 시작 userId={}, petId={}", user.getUserId(), pet.getPetId());

    double latitude = SUWON_LAT;
    double longitude = SUWON_LNG;
    // todo : 추후 사용자 실제 위치 필드로 교체
    log.info("[추천 파이프라인-알림] 사용자 위경도 lat={}, lng={}", latitude, longitude);

    // 1. 현재 좌표를 기상청 격자 좌표로 변환한다.
    WeatherGridPoint gridPoint = weatherGridConverter.convertToGrid(latitude, longitude);
    int nx = gridPoint.getNx();
    int ny = gridPoint.getNy();
    log.info("[추천 파이프라인-알림] 위경도 -> 기상청 격자 변환 결과 lat={}, lng={}, nx={}, ny={}",
            latitude, longitude, nx, ny);

    // 2. 격자 좌표 기준 현재 날씨를 조회한다.
    WeatherContext weatherContext = weatherService.getOrLoadWeather(nx, ny);
    log.info(
            "[추천 파이프라인-알림] 날씨 요약 정보 walkLevel={}, temp={}, humidity={}, precipitationType={}, rainfall={}, windSpeed={}",
            weatherContext.getWalkLevel(),
            weatherContext.getTemperature(),
            weatherContext.getHumidity(),
            weatherContext.getPrecipitationType(),
            weatherContext.getRainfall(),
            weatherContext.getWindSpeed()
    );

    // 3. 사용자/반려견/날씨 기준 추천 장소 후보를 수집한다.
    List<Place> candidates = candidatePlaceService.getInitialCandidates(
            user,
            pet,
            weatherContext,
            latitude,
            longitude
    );
    log.info("[추천 파이프라인-알림] 장소 후보 개수 및 상위 이름 count={}, topNames={}",
            candidates.size(),
            summarizePlaceNames(candidates, LOG_TOP_PLACE_LIMIT));

    if (candidates.isEmpty()) {
      log.warn("[추천 파이프라인-알림] 후보 장소 없음 userId={}, petId={}, walkLevel={}",
              user.getUserId(),
              pet.getPetId(),
              weatherContext.getWalkLevel());
      return RecommendationNotificationResult.builder()
              .userId(user.getUserId())
              .petId(pet.getPetId())
              .petName(pet.getPetName())
              .weatherType(resolveWeatherType(weatherContext))
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(null)
              .message("추천 가능한 장소가 없습니다.")
              .fallbackUsed(false)
              .cacheHit(false)
              .build();
    }

    // 4. RAG 문맥 조회
    String ragContext = ragService.searchContext(pet, weatherContext);
    log.info("[추천 파이프라인-알림] RAG 문맥 길이 및 요약 length={}, preview={}",
            ragContext == null ? 0 : ragContext.length(),
            abbreviate(ragContext, 160));

    // 5. 후보 장소를 점수화하고 최종 순위를 계산한다.
    List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
            candidates,
            user,
            pet,
            weatherContext,
            latitude,
            longitude
    );

    log.info("[추천 파이프라인-알림] 점수 계산 결과 상위 장소 count={}, topScores={}",
            rankedPlaces.size(),
            summarizeScoredPlaces(rankedPlaces, LOG_TOP_PLACE_LIMIT));

    if (rankedPlaces.isEmpty()) {
      log.warn("[추천 파이프라인-알림] 점수 결과 없음 userId={}, petId={}, candidateCount={}",
              user.getUserId(),
              pet.getPetId(),
              candidates.size());
      return RecommendationNotificationResult.builder()
              .userId(user.getUserId())
              .petId(pet.getPetId())
              .petName(pet.getPetName())
              .weatherType(resolveWeatherType(weatherContext))
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(null)
              .message("추천 가능한 장소가 없습니다.")
              .fallbackUsed(false)
              .cacheHit(false)
              .build();
    }

    Place topPlace = rankedPlaces.stream()
            .map(ScoredPlace::getPlace)
            .findFirst()
            .orElse(null);

    // 6. 개인화 정보와 RAG context를 합쳐 생성용 프롬프트를 만든다.
    String prompt = recommendationPromptService.buildRecommendationPrompt(
            user,
            pet,
            weatherContext,
            rankedPlaces,
            ragContext
    );
    log.info("[추천 파이프라인-알림] 프롬프트 길이 및 핵심 정보 length={}, preview={}",
            prompt.length(),
            abbreviate(prompt, 200));

    // 7. 로그 저장용 상위 추천 장소 요약
    String recommendedPlaces = buildTopPlaceSummary(rankedPlaces);
    log.info("[추천 파이프라인-알림] 상위 장소 요약 summary={}", recommendedPlaces);

    try {
      // 8. 동일 프롬프트에 대한 캐시 키 생성
      String cacheKey = geminiCacheService.generateKey(prompt);
      log.info("[추천 파이프라인-알림] Gemini cache key 생성 key={}", cacheKey);

      // 9. 캐시 조회
      String cachedResponse = geminiCacheService.get(cacheKey);

      if (cachedResponse != null) {
        log.info("[추천 파이프라인-알림] Gemini cache hit responseLength={}, generateRecommendation호출={}",
                cachedResponse.length(),
                false);

        aiLogservice.save(
                user,
                pet,
                prompt,
                recommendedPlaces,
                ragContext,
                cachedResponse,
                false,
                true,
                0L
        );

        log.info("[추천 파이프라인-알림] AI 로그 저장 완료 fallbackUsed={}, cacheHit={}, latencyMs={}",
                false, true, 0L);

        return RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType(resolveWeatherType(weatherContext))
                .weatherSummary(buildWeatherSummary(weatherContext))
                .place(topPlace)
                .message(cachedResponse)
                .fallbackUsed(false)
                .cacheHit(true)
                .build();
      }

      log.info("[추천 파이프라인-알림] Gemini cache miss generateRecommendation호출={}", true);

      // 10. Gemini 호출
      long startTime = System.currentTimeMillis();
      String geminiMessage = geminiRecommendationService.generateRecommendation(prompt);
      long latencyMs = System.currentTimeMillis() - startTime;
      boolean fallbackUsed = geminiRecommendationService.isFallbackResponse(geminiMessage);

      log.info("[추천 파이프라인-알림] Gemini 응답 생성 여부 created={}, fallbackUsed={}, responseLength={}, latencyMs={}",
              geminiMessage != null && !geminiMessage.isBlank(),
              fallbackUsed,
              geminiMessage == null ? 0 : geminiMessage.length(),
              latencyMs);

      if (fallbackUsed) {
        log.warn("[추천 파이프라인-알림] fallback 여부 used={}, reason={}", true, "fallback_message_detected");
      }

      // 11. 캐시 저장
      geminiCacheService.save(cacheKey, geminiMessage);

      // 12. 성공 로그 저장
      aiLogservice.save(
              user,
              pet,
              prompt,
              recommendedPlaces,
              ragContext,
              geminiMessage,
              fallbackUsed,
              false,
              latencyMs
      );

      log.info("[추천 파이프라인-알림] AI 로그 저장 완료 fallbackUsed={}, cacheHit={}, latencyMs={}",
              fallbackUsed, false, latencyMs);

      return RecommendationNotificationResult.builder()
              .userId(user.getUserId())
              .petId(pet.getPetId())
              .petName(pet.getPetName())
              .weatherType(resolveWeatherType(weatherContext))
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(topPlace)
              .message(geminiMessage)
              .fallbackUsed(fallbackUsed)
              .cacheHit(false)
              .build();

    } catch (Exception e) {
      log.error("[추천 파이프라인-알림] Gemini 호출 실패", e);

      String fallbackResponse = "현재 추천 문장을 생성하는 중 문제가 발생했습니다. 추천 장소 정보를 먼저 확인해주세요.";

      aiLogservice.save(
              user,
              pet,
              prompt,
              recommendedPlaces,
              ragContext,
              fallbackResponse,
              true,
              false,
              0L
      );

      log.info("[추천 파이프라인-알림] AI 로그 저장 완료 fallbackUsed={}, cacheHit={}, latencyMs={}",
              true, false, 0L);

      return RecommendationNotificationResult.builder()
              .userId(user.getUserId())
              .petId(pet.getPetId())
              .petName(pet.getPetName())
              .weatherType(resolveWeatherType(weatherContext))
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(topPlace)
              .message(fallbackResponse)
              .fallbackUsed(true)
              .cacheHit(false)
              .build();
    }
  }

  private String resolveWeatherType(WeatherContext weatherContext) {
    if (weatherContext == null) {
      return "SUNNY";
    }

    if (weatherContext.isRaining()) {
      return "RAINY";
    }

    if (weatherContext.isHot()) {
      return "HEATWAVE";
    }

    if (weatherContext.isCold()) {
      return "COLD_WAVE";
    }

    String precipitationType = weatherContext.getPrecipitationType();
    if (precipitationType != null && !precipitationType.isBlank() && !"NONE".equalsIgnoreCase(precipitationType)) {
      return "CLOUDY";
    }

    return "SUNNY";
  }

  private String buildWeatherSummary(WeatherContext weatherContext) {
    if (weatherContext == null) {
      return "";
    }

    return String.format(
            "산책지수 %s, 기온 %.1f도, 습도 %d%%, 강수 %s, 강수량 %.1fmm, 풍속 %.1fm/s",
            weatherContext.getWalkLevel(),
            weatherContext.getTemperature(),
            weatherContext.getHumidity(),
            weatherContext.getPrecipitationType(),
            weatherContext.getRainfall(),
            weatherContext.getWindSpeed()
    );
  }

  /**
   * 상위 추천 장소를 로그 저장용 문자열로 요약한다.
   *
   * @param rankedPlaces 점수순으로 정렬된 장소 목록
   * @return 상위 3개 장소 요약 문자열
   */
  private String buildTopPlaceSummary(List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "추천 장소 없음";
    }

    // 1. 상위 3개 장소만 선택한다.
    // 2. 장소명과 총점을 한 줄 요약 문자열로 변환한다.
    return rankedPlaces.stream()
            .limit(3)
            .map(scoredPlace -> {
              Place place = scoredPlace.getPlace();
              return place.getTitle() + "(" + scoredPlace.getTotalScore() + "점)";
            })
            .collect(Collectors.joining(", "));
  }

  /**
   * 장소 목록을 요약하는 문자열로 변환한다.
   *
   * @param places 장소 목록
   * @param limit 요약할 장소 수 제한
   * @return 요약된 장소 목록 문자열
   */
  private String summarizePlaceNames(List<Place> places, int limit) {
    if (places == null || places.isEmpty()) {
      return "[]";
    }

    return places.stream()
            .limit(limit)
            .map(place -> place == null ? "null" : place.getTitle())
            .collect(Collectors.joining(", ", "[", "]"));
  }

  /**
   * 장소 목록을 요약하는 문자열로 변환한다.
   *
   * @param rankedPlaces 장소 점수 목록
   * @param limit 요약할 장소 수 제한
   * @return 요약된 장소 목록 문자열
   */
  private String summarizeScoredPlaces(List<ScoredPlace> rankedPlaces, int limit) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "[]";
    }

    return rankedPlaces.stream()
            .limit(limit)
            .map(scoredPlace -> {
              Place place = scoredPlace.getPlace();
              String title = place == null ? "null" : place.getTitle();
              return title + "|" + scoredPlace.getTotalScore() + "|" + abbreviate(scoredPlace.getSummary(), 80);
            })
            .collect(Collectors.joining(", ", "[", "]"));
  }

  /**
   * 문자열을 지정된 길이로 약식하여 반환한다.
   *
   * @param value 원본 문자열
   * @param maxLength 약식할 최대 길이
   * @return 약식된 문자열
   */
  private String abbreviate(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
  }
}
