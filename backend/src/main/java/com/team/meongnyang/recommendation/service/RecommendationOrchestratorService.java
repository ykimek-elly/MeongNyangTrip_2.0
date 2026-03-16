package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.rag.service.RagService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherGridPoint;
import com.team.meongnyang.recommendation.cache.WeatherCacheService;
import com.team.meongnyang.recommendation.weather.service.WeatherGridConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 반려견 추천 흐름 전체를 조합하는 오케스트레이션 서비스다.
 *
 * <p>사용자, 반려견, 날씨, 장소 후보, RAG context를 모아
 * 최종 추천 프롬프트를 만들고 Gemini 응답을 생성한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RecommendationOrchestratorService {
  private static final int LOG_TOP_PLACE_LIMIT = 3;

  private final RecommendationUserReader recommendationUserReader;
  private final RecommnedationPetReader recommnedationPetReader;
  private final WeatherCacheService weatherService;
  @Qualifier("orchestratorPlaceService")
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

  /**
   * 현재 로그인 사용자를 기준으로 반려견 동반 추천 문장을 생성한다.
   *
   * @param email 현재 사용자 이메일
   * @return 추천 문장 또는 fallback 메시지
   */
  public String recommendForCurrentUser(String email) {
    // 1. 현재 로그인 사용자를 조회한다.
    User user = recommendationUserReader.getCurrentUserByEmail(email);
    log.info("[추천 오케스트레이션] 사용자 조회 결과 email={}, userId={}, nickname={}", email, user.getUserId(), user.getNickname());
    log.info("[추천 오케스트레이션] 사용자 위경도 lat={}, lng={}", SUWON_LAT, SUWON_LNG);
    // todo : 사용자로부터 위치를 받는지, 위치 데이터가 따로 넘어오는지, 현재는 임의로 정해둔 SUWON LAT,LNG

    // 2. 대표 반려견 정보를 조회한다.
    Pet pet = recommnedationPetReader.getPrimaryPet(user);
    log.info("[추천 오케스트레이션] 대표 반려견 조회 결과 petId={}, name={}, preferredPlace={}", pet.getPetId(), pet.getPetName(), pet.getPreferredPlace());

    // 3. 현재 좌표를 기상청 격자 좌표로 변환한다.
    WeatherGridPoint gridPoint = weatherGridConverter.convertToGrid(SUWON_LAT, SUWON_LNG);
    int nx = gridPoint.getNx();
    int ny = gridPoint.getNy();
    log.info("[추천 오케스트레이션] 위경도 -> 기상청 격자 변환 결과 lat={}, lng={}, nx={}, ny={}", SUWON_LAT, SUWON_LNG, nx, ny);

    // 4. 격자 좌표 기준 현재 날씨를 조회한다.
    WeatherContext weatherContext = weatherService.getOrLoadWeather(nx, ny);
    log.info(
            "[추천 오케스트레이션] 날씨 요약 정보 walkLevel={}, temp={}, humidity={}, precipitationType={}, rainfall={}, windSpeed={}",
            weatherContext.getWalkLevel(),
            weatherContext.getTemperature(),
            weatherContext.getHumidity(),
            weatherContext.getPrecipitationType(),
            weatherContext.getRainfall(),
            weatherContext.getWindSpeed()
    );
    // 5. 사용자/반려견/날씨 기준 추천 장소 후보를 수집한다.
    List<Place> candidates = candidatePlaceService.getInitialCandidates(
            user,
            pet,
            weatherContext,
            SUWON_LAT,
            SUWON_LNG
    );
    log.info("[추천 오케스트레이션] 장소 후보 개수 및 상위 이름 count={}, topNames={}", candidates.size(), summarizePlaceNames(candidates, LOG_TOP_PLACE_LIMIT));
    if (candidates.isEmpty()) {
      log.warn("[추천 오케스트레이션] 후보 장소 없음으로 추천 중단 userId={}, petId={}, walkLevel={}",
              user.getUserId(),
              pet.getPetId(),
              weatherContext.getWalkLevel());
    }
    String ragContext = ragService.searchContext(pet, weatherContext);

    // 7. 후보 장소를 점수화하고 최종 순위를 계산한다.
    log.info("[추천 오케스트레이션] RAG 문맥 길이 및 요약 length={}, preview={}", ragContext == null ? 0 : ragContext.length(), abbreviate(ragContext, 160));
    List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
            candidates,
            user,
            pet,
            weatherContext,
            SUWON_LAT,
            SUWON_LNG
    );

    log.info("[추천 오케스트레이션] 점수 계산 결과 상위 장소 count={}, topScores={}", rankedPlaces.size(), summarizeScoredPlaces(rankedPlaces, LOG_TOP_PLACE_LIMIT));
    if (rankedPlaces.isEmpty()) {
      log.warn("[추천 오케스트레이션] 점수 결과 없음 userId={}, petId={}, candidateCount={}",
              user.getUserId(),
              pet.getPetId(),
              candidates.size());
    }
    if (rankedPlaces.isEmpty()) {
      log.warn("추천 가능한 장소가 없습니다.");
      return "추천 가능한 장소가 없습니다.";
    }

    // 8. 개인화 정보와 RAG context를 합쳐 생성용 프롬프트를 만든다.
    String prompt = recommendationPromptService.buildRecommendationPrompt(
            user,
            pet,
            weatherContext,
            rankedPlaces,
            ragContext
    );
    log.info("개인 데이터 조합 프롬프트: {}", prompt);

    // 9. 로그 저장용 상위 추천 장소 요약을 만든다.
    log.info("[추천 오케스트레이션] 프롬프트 길이 및 핵심 정보 length={}, preview={}", prompt.length(), abbreviate(prompt, 200));
    String recommendedPlaces = buildTopPlaceSummary(rankedPlaces);
    log.info("[추천 오케스트레이션] 상위 장소 요약 summary={}", recommendedPlaces);

    try {
      // 10. 동일 프롬프트에 대한 캐시 키를 생성한다.
      String cacheKey = geminiCacheService.generateKey(prompt);
      log.info("[추천 오케스트레이션] Gemini cache key 생성 key={}", cacheKey);
      // 11. 캐시에 저장된 AI 응답이 있는지 확인한다.
      String cachedResponse = geminiCacheService.get(cacheKey);

      if (cachedResponse != null) {
        log.info("[추천 오케스트레이션] Gemini cache hit responseLength={}, generateRecommendation호출={}", cachedResponse.length(), false);
        log.info("Gemini 캐시 응답 반환");
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
        log.info("[추천 오케스트레이션] AI 로그 저장 완료 fallbackUsed={}, cacheHit={}, latencyMs={}", false, true, 0L);
        log.info("[추천 오케스트레이션] 최종 반환 직전 요약 source={}, fallbackUsed={}, cacheHit={}, rankedCount={}, topSummary={}",
                "cache",
                false,
                true,
                rankedPlaces.size(),
                recommendedPlaces);
        return cachedResponse;
      }
      log.info("[추천 오케스트레이션] Gemini cache miss generateRecommendation호출={}", true);

      // 12. 캐시 미스면 Gemini로 추천 문장을 생성한다.
      long startTime = System.currentTimeMillis();
      String geminiMessage = geminiRecommendationService.generateRecommendation(prompt);
      long latencyMs = System.currentTimeMillis() - startTime;
      boolean fallbackUsed = geminiRecommendationService.isFallbackResponse(geminiMessage);
      log.info("[추천 오케스트레이션] Gemini 응답 생성 여부 created={}, fallbackUsed={}, responseLength={}, latencyMs={}",
              geminiMessage != null && !geminiMessage.isBlank(),
              fallbackUsed,
              geminiMessage == null ? 0 : geminiMessage.length(),
              latencyMs);
      if (fallbackUsed) {
        log.warn("[추천 오케스트레이션] fallback 여부 used={}, reason={}", true, "fallback_message_detected");
      }
      log.info("Gemini 응답: {}", geminiMessage);

      // 13. 새로 생성한 응답을 캐시에 저장한다.
      geminiCacheService.save(cacheKey, geminiMessage);

      // 14. 성공 로그를 저장한다.
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

      log.info("[추천 오케스트레이션] AI 로그 저장 완료 fallbackUsed={}, cacheHit={}, latencyMs={}", fallbackUsed, false, latencyMs);
      log.info("[추천 오케스트레이션] 최종 반환 직전 요약 source={}, fallbackUsed={}, cacheHit={}, rankedCount={}, topSummary={}",
              "gemini",
              fallbackUsed,
              false,
              rankedPlaces.size(),
              recommendedPlaces);
      return geminiMessage;
    } catch (Exception e) {
      log.error("Gemini 호출 실패", e);
      log.warn("[추천 오케스트레이션] Gemini 실패로 fallback 전환 errorType={}", e.getClass().getSimpleName());

      // 15. 생성 실패 시 사용자에게 반환할 fallback 메시지를 만든다.
      String fallbackResponse = "현재 추천 문장을 생성하는 중 문제가 발생했습니다. 추천 장소 정보를 먼저 확인해주세요.";

      // 16. 실패 로그를 저장한다.
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
      log.info("[추천 오케스트레이션] AI 로그 저장 완료 fallbackUsed={}, cacheHit={}, latencyMs={}", true, false, 0L);
      log.info("[추천 오케스트레이션] 최종 반환 직전 요약 source={}, fallbackUsed={}, cacheHit={}, rankedCount={}, topSummary={}",
              "exception_fallback",
              true,
              false,
              rankedPlaces.size(),
              recommendedPlaces);
      return fallbackResponse;
    }
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
