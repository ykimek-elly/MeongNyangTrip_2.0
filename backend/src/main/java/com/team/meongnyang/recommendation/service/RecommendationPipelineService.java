package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.context.service.RecommendationEvidenceContextService;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.log.RecommendationBatchTraceContext;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RecommendationPipelineService {
  private static final int LOG_TOP_PLACE_LIMIT = 3;
  private static final String NOTIFICATION_SUMMARY_HEADER = "[알림요약]";
  private static final String DEFAULT_NOTIFICATION_SUMMARY = "오늘 컨디션에 맞는 장소로 가볍게 다녀와 보세요!";

  private final RecommendationUserReader recommendationUserReader;
  private final RecommendationPetReader recommendationPetReader;
  private final WeatherCacheService weatherService;
  private final CandidatePlaceService candidatePlaceService;
  private final PlaceScoringService placeScoringService;
  private final RecommendationEvidenceContextService recommendationEvidenceContextService;
  private final RecommendationPromptService recommendationPromptService;
  private final GeminiCacheService geminiCacheService;
  private final GeminiRecommendationService geminiRecommendationService;
  private final WeatherGridConverter weatherGridConverter;
  private final AiLogService aiLogservice;

  private static final double SUWON_LAT = 37.27;
  private static final double SUWON_LNG = 127.01;

  /**
   * 수동 진입시
   * @param email
   * @return
   */
  public RecommendationNotificationResult recommendForCurrentUser(String email) {
    log.info("[추천 파이프라인] START");

    User user = recommendationUserReader.getCurrentUserByEmail(email);
    log.info("[추천 파이프라인] 사용자 조회 결과 email={}, userId={}, nickname={}", email, user.getUserId(), user.getNickname());
    log.info("[추천 파이프라인] 사용자 위경도 lat={}, lng={}", SUWON_LAT, SUWON_LNG);

    Pet pet = recommendationPetReader.getPrimaryPet(user);
    log.info("[추천 파이프라인] 대표 반려견 조회 결과 petId={}, name={}, preferredPlace={}", pet.getPetId(), pet.getPetName(), pet.getPreferredPlace());

    return recommendForNotification(user, pet);
  }

  /**
   * 배치 스케줄러 알림 진입시
   * @param user
   * @param pet
   * @return
   */
  public RecommendationNotificationResult recommendForNotification(User user, Pet pet) {
    return recommendForNotification(user, pet, null);
  }

  /**
   * 배치 실행 시에는 batchExecutionId를 함께 전달해서 사용자 단위 추적을 강화한다.
   */
  public RecommendationNotificationResult recommendForNotification(User user, Pet pet, String batchExecutionId) {
    try (RecommendationBatchTraceContext.TraceScope ignored =
                 RecommendationBatchTraceContext.open(batchExecutionId, user.getUserId(), pet.getPetId())) {
    log.info("[추천 파이프라인-알림] 시작 userId={}, petId={}", user.getUserId(), pet.getPetId());

    double latitude = SUWON_LAT;
    double longitude = SUWON_LNG;
    log.info("[추천 파이프라인-알림] 사용자 위경도 lat={}, lng={}", latitude, longitude);

    WeatherGridPoint gridPoint = weatherGridConverter.convertToGrid(latitude, longitude);
    int nx = gridPoint.getNx();
    int ny = gridPoint.getNy();
    log.info("[추천 파이프라인-알림] 위경도 -> 기상청 격자 변환 결과 lat={}, lng={}, nx={}, ny={}",
            latitude, longitude, nx, ny);

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
      log.warn("[추천 파이프라인-알림] 후보 장소 없음");

      return RecommendationNotificationResult.builder()
              .userId(user.getUserId())
              .petId(pet.getPetId())
              .petName(pet.getPetName())
              .weatherType(resolveWeatherType(weatherContext))
              .weatherWalkLevel(weatherContext.getWalkLevel())
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(null)
              .message("추천 가능한 장소가 없습니다. 인기 있는 장소를 추천합니다.")
              .fallbackUsed(false)
              .cacheHit(false)
              .build();
    }

    // 1. 최근 추천 이력을 조회해 다양성 패널티 후보를 준비한다.
    Map<Long, AiLogService.RecommendationDiversityPenalty> diversityPenalties = loadDiversityPenalties(user);

    // 2. 기존 점수 + 다양성 패널티를 함께 반영해 최종 순위를 계산한다.
    List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
            candidates,
            user,
            pet,
            weatherContext,
            latitude,
            longitude,
            diversityPenalties
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
              .weatherWalkLevel(weatherContext.getWalkLevel())
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

    RecommendationEvidenceContext evidenceContext = recommendationEvidenceContextService.buildContext(
            user,
            pet,
            weatherContext,
            rankedPlaces
    );
    log.info("[추천 파이프라인-알림] 추천 컨텍스트 길이 및 요약 length={}, preview={}",
            evidenceContext.getContextSnapshot().length(),
            RecommendationTextUtils.abbreviate(evidenceContext.getContextSnapshot(), 200));

    String prompt = recommendationPromptService.buildRecommendationPrompt(evidenceContext);
    log.info("[추천 파이프라인-알림] 프롬프트 길이 및 핵심 정보 length={}, preview={}",
            prompt.length(),
            RecommendationTextUtils.abbreviate(prompt, 200));

    String recommendedPlaces = buildTopPlaceSummary(rankedPlaces);
    log.info("[추천 파이프라인-알림] 상위 장소 요약 summary={}", recommendedPlaces);

    try {
      String cacheKey = geminiCacheService.generateKey(prompt);
      log.info("[추천 파이프라인-알림] Gemini cache key 생성 key={}", cacheKey);

      String cachedResponse = geminiCacheService.get(cacheKey);

      if (cachedResponse != null) {
        String notificationSummary = extractNotificationSummary(cachedResponse);
        if (notificationSummary == null || notificationSummary.isBlank()) {
          notificationSummary = DEFAULT_NOTIFICATION_SUMMARY;
        }
        log.info("[추천 파이프라인-알림] Gemini cache hit responseLength={}, generateRecommendation호출={}",
                cachedResponse.length(),
                false);

        aiLogservice.save(
                user,
                pet,
                prompt,
                recommendedPlaces,
                topPlace == null ? null : topPlace.getId(),
                evidenceContext.getContextSnapshot(),
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
                .weatherWalkLevel(weatherContext.getWalkLevel())
                .weatherSummary(buildWeatherSummary(weatherContext))
                .place(topPlace)
                .message(notificationSummary)
                .fallbackUsed(false)
                .cacheHit(true)
                .aiResponse(cachedResponse)
                .geminiCacheKey(cacheKey)
                .build();
      }

      log.info("[추천 파이프라인-알림] Gemini cache miss");

      long startTime = System.currentTimeMillis();
      String geminiMessage = geminiRecommendationService.generateRecommendation(prompt);
      String notificationSummary = extractNotificationSummary(geminiMessage);
      if (notificationSummary == null || notificationSummary.isBlank()) {
        notificationSummary = DEFAULT_NOTIFICATION_SUMMARY;
      }
      long latencyMs = System.currentTimeMillis() - startTime;
      boolean fallbackUsed = geminiRecommendationService.isFallbackResponse(geminiMessage);

      if (fallbackUsed) {
        log.warn("[추천 파이프라인-알림] fallback 여부 used={}", true);
      }
      if (!fallbackUsed) {
        geminiCacheService.save(cacheKey, geminiMessage);
      }

      aiLogservice.save(
              user,
              pet,
              prompt,
              recommendedPlaces,
              topPlace == null ? null : topPlace.getId(),
              evidenceContext.getContextSnapshot(),
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
              .weatherWalkLevel(weatherContext.getWalkLevel())
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(topPlace)
              .message(notificationSummary)
              .fallbackUsed(fallbackUsed)
              .cacheHit(false)
              .aiResponse(geminiMessage)
              .geminiCacheKey(cacheKey)
              .build();

    } catch (Exception e) {
      log.error("[추천 파이프라인-알림] Gemini 호출 실패", e);

      String fallbackResponse = DEFAULT_NOTIFICATION_SUMMARY;
      aiLogservice.save(
              user,
              pet,
              prompt,
              recommendedPlaces,
              topPlace == null ? null : topPlace.getId(),
              evidenceContext.getContextSnapshot(),
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
              .weatherWalkLevel(weatherContext.getWalkLevel())
              .weatherSummary(buildWeatherSummary(weatherContext))
              .place(topPlace)
              .message(fallbackResponse)
              .fallbackUsed(true)
              .cacheHit(false)
              .aiResponse(fallbackResponse)
              .geminiCacheKey(null)
              .build();
    }
    }
  }

  /**
   * 날씨 컨텍스트를 알림/응답에서 사용하는 날씨 타입 코드로 변환한다.
   *
   * @param weatherContext 현재 날씨 정보
   * @return 날씨 타입 코드
   */
  private String resolveWeatherType(WeatherContext weatherContext) {
    if (weatherContext == null) {
      return "SUNNY";
    }

    if (weatherContext.isRaining()) {
      return "RAIN";
    }

    if (weatherContext.isHot()) {
      return "HOT";
    }

    if (weatherContext.isCold()) {
      return "COLD";
    }

    String walkLevel = weatherContext.getWalkLevel();
    if (walkLevel != null && ("CAUTION".equalsIgnoreCase(walkLevel) || "DANGEROUS".equalsIgnoreCase(walkLevel))) {
      return "CLOUDY";
    }

    return "SUNNY";
  }

  /**
   * 현재 날씨 정보를 사용자에게 보여줄 한 줄 요약 문자열로 만든다.
   *
   * @param weatherContext 현재 날씨 정보
   * @return 날씨 요약 문자열
   */
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
   * 상위 추천 장소 목록을 AI 로그 저장용 요약 문자열로 변환한다.
   *
   * @param rankedPlaces 점수 계산이 완료된 장소 목록
   * @return 상위 추천 장소 요약 문자열
   */
  private String buildTopPlaceSummary(List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "추천 장소 없음";
    }

    return rankedPlaces.stream()
            .limit(3)
            .map(scoredPlace -> {
              Place place = scoredPlace.getPlace();
              return place.getTitle() + "(" + scoredPlace.getTotalScore() + "점)";
            })
            .collect(Collectors.joining(", "));
  }

  private Map<Long, AiLogService.RecommendationDiversityPenalty> loadDiversityPenalties(User user) {
    if (user == null) {
      return Map.of();
    }

    Map<Long, AiLogService.RecommendationDiversityPenalty> penalties =
            aiLogservice.getRecentRecommendedPlacePenalties(user.getUserId());
    if (penalties == null || penalties.isEmpty()) {
      return Map.of();
    }

    log.info("[추천 파이프라인-알림] 다양성 패널티 대상 count={}, userId={}",
            penalties.size(),
            user.getUserId());
    return penalties;
  }

  /**
   * 후보 장소 목록에서 상위 일부 이름만 추려 로그용 문자열로 만든다.
   *
   * @param places 후보 장소 목록
   * @param limit 포함할 최대 개수
   * @return 장소명 요약 문자열
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
   * 점수 계산 결과에서 상위 일부 장소의 이름, 점수, 요약을 추려 로그 문자열로 만든다.
   *
   * @param rankedPlaces 점수 계산이 완료된 장소 목록
   * @param limit 포함할 최대 개수
   * @return 점수 결과 요약 문자열
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
              return title + "|" + scoredPlace.getTotalScore() + "|" + RecommendationTextUtils.abbreviate(scoredPlace.getSummary(), 80);
            })
            .collect(Collectors.joining(", ", "[", "]"));
  }



  /**
   * Gemini 전체 응답에서 알림 본문에 사용할 요약 섹션의 첫 줄만 추출한다.
   *
   * @param geminiResponse Gemini가 반환한 전체 응답 문자열
   * @return 알림용 한 줄 요약, 없으면 {@code null}
   */
  private String extractNotificationSummary(String geminiResponse) {
    if (geminiResponse == null || geminiResponse.isBlank()) {
      return null;
    }

    int summaryHeaderIndex = geminiResponse.indexOf(NOTIFICATION_SUMMARY_HEADER);
    if (summaryHeaderIndex < 0) {
      return null;
    }

    String summarySection = geminiResponse.substring(summaryHeaderIndex + NOTIFICATION_SUMMARY_HEADER.length()).trim();
    if (summarySection.isBlank()) {
      return null;
    }

    int nextSectionIndex = summarySection.indexOf("\n[");
    if (nextSectionIndex >= 0) {
      summarySection = summarySection.substring(0, nextSectionIndex).trim();
    }

    String firstLine = summarySection.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .findFirst()
            .orElse(null);

    if (firstLine == null || firstLine.isBlank()) {
      return null;
    }
    firstLine = firstLine.replaceFirst("^[-•]\\s*", "").trim();
    return firstLine;
  }
}
