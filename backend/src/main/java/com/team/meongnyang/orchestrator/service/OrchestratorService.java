package com.team.meongnyang.orchestrator.service;

import com.team.meongnyang.ai.service.AiLogservice;
import com.team.meongnyang.ai.service.GeminiCacheService;
import com.team.meongnyang.orchestrator.dto.ScoredPlace;
import com.team.meongnyang.weather.dto.WeatherContext;
import com.team.meongnyang.weather.dto.WeatherGridPoint;
import com.team.meongnyang.weather.service.WeatherCacheService;
import com.team.meongnyang.weather.service.WeatherGridConverter;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 추천 오케스트라 서비스
 * 하나의 유즈케이스
 *
 * 흐름:
 * 사용자 조회 → 반려견 조회 → 날씨 조회 → 장소 후보 조회
 * → RAG 문서 검색 → 가중치 계산 → 프롬프트 생성
 * → Gemini 응답 생성 → 결과 저장 → 알림 발송
 *
 * 주의:
 * - 이 서비스는 전체 흐름만 조립한다.
 * - 세부 로직(조회, 계산, 프롬프트 생성, AI 호출)은 각 전용 서비스에 위임한다.
 * - RecommendService 안에서 직접 점수 계산, 프롬프트 작성, API 세부 호출을 하지 않는다.
 */

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrchestratorService {

  private final OrchUserService orchUserService;
  private final OrchDogService orchDogService;
  private final WeatherCacheService weatherService;
  @Qualifier("orchestratorPlaceService")
  private final OrchPlaceService orchPlaceService;
  private final RagService ragService;
  private final PlaceScoreService placeScoreService;
  private final PromptService promptService;
  private final GeminiCacheService geminiCacheService;
  private final GeminiService geminiService;
  private final WeatherGridConverter weatherGridConverter;

  // 임시 좌표 (수원)
  private static final double SUWON_LAT = 37.27;
  private static final double SUWON_LNG = 127.01;
  private final AiLogservice aiLogservice;

  public String recommend (Long id) {
    // 1. 사용자 조회
    User user = orchUserService.getUserById(id);
    log.info("조회된 사용자 닉네임 : {}", user.getNickname());
    log.info("조회된 위치 lat : {} , lng : {} ", SUWON_LAT, SUWON_LNG);

    // 2. 반려견 정보 조회
    Dog dog = orchDogService.getPrimaryDog(user);
    log.info("조회된 반려견 이름 : {}", dog.getDogName());
    log.info("조회된 반려견 성격 : {}", dog.getPersonality());

    // 3. 위치 기준 날씨 조회 (서울)
      // 3-1 좌표값을 nx, ny 값으로 변환
    WeatherGridPoint gridPoint = weatherGridConverter.convertToGrid(SUWON_LAT, SUWON_LNG);
    int nx = gridPoint.getNx();
    int ny = gridPoint.getNy();

      // 3-2 변환한 값으로 날씨 조회
    WeatherContext weatherContext = weatherService.getOrLoadWeather(nx,ny);
    log.info("조회된 날씨 : {}", weatherContext.getWalkLevel());

    // 4. 추천 가능 장소 조회 (유저 저장된 위치 + 반려견 선호 장소)
    List<Place> candidates = orchPlaceService.getInitialCandidates(user,dog,weatherContext,SUWON_LAT,SUWON_LNG);
    log.info("조회된 장소 개수 : {}", candidates.size());


    // 5. RAG 조회 (강아지 + 날씨)
    String ragQuery = buildRagQuery(dog, weatherContext);
    String ragContext = ragService.searchContext(ragQuery);
    log.info("RAG 질문 : {}", ragQuery);
    if (ragContext == null || ragContext.isBlank()) {
      log.info("RAG 검색 결과가 없습니다.");
    } else {
      log.info("RAG 검색 결과 : {}", ragContext.substring(0, Math.min(300, ragContext.length())));
    }
    // 6. Place 점수 계산 및 정렬
    List<ScoredPlace> rankedPlaces = placeScoreService.scorePlace(candidates, user, dog, weatherContext);
    if (rankedPlaces.isEmpty()) {
      log.warn("추천 가능한 장소가 없습니다");
      return "추천 가능한 장소가 없습니다.";
    }
    log.info("1순위 장소 : {}", rankedPlaces.getFirst().getPlace().getTitle());

    // 7. Prompt
    String prompt = promptService.buildRecommendationPrompt(user, dog, weatherContext, rankedPlaces, ragContext);
    log.info("개인 데이터 조합 프롬프트 : {} ", prompt);

    // 추천 장소 요약 (로그 저장용)
    String recommendedPlaces = buildTopPlaceSummary(rankedPlaces);
    // 8. Gemini
    try {
      String cacheKey = geminiCacheService.generateKey(prompt);
      // 1. Redis 캐시 조회
      String cachedResponse = geminiCacheService.get(cacheKey);
      if (cachedResponse != null) {
        log.info("Gemini 캐시 응답 반환");
        aiLogservice.save(
                user,
                dog,
                prompt,
                recommendedPlaces,
                ragContext,
                cachedResponse,
                false,   // fallbackUsed
                true,    // cacheHit
                0L
        );

        return cachedResponse;
      }
      // 2. 캐시 없으면 실제 Gemini 호출

      long startTime = System.currentTimeMillis();
      String geminiMessage = geminiService.generateRecommendation(prompt);
      long latencyMs = System.currentTimeMillis() - startTime;
      log.info("Gemini 응답 : {}", geminiMessage);

      // 3. Redis 저장
      geminiCacheService.save(cacheKey, geminiMessage);

      // 4. 로그 저장
      aiLogservice.save(
              user,
              dog,
              prompt,
              recommendedPlaces,
              ragContext,
              geminiMessage,
              false,   // fallbackUsed
              false,   // cacheHit
              latencyMs
      );

      // 9. 필요 시 카카오 알림톡 발송
      // kakaoService.send(...);
      return geminiMessage;

    } catch (Exception e) {
      log.error("Gemini 호출 실패", e);

      String fallbackResponse = "현재 추천 문장을 생성하는 중 문제가 발생했습니다. 추천 장소 정보를 먼저 확인해주세요.";

      // 8. 실패/fallback 로그 저장  ← 여기
      aiLogservice.save(
              user,
              dog,
              prompt,
              recommendedPlaces,
              ragContext,
              fallbackResponse,
              true,    // fallbackUsed
              false,   // cacheHit
              0L
      );
      return fallbackResponse;
    }


  }

  private String buildRagQuery(Dog dog, WeatherContext weather) {
    return """
            견종별 반려견 산책 주의사항 건강관리
            견종 %s
            크기 %s
            기온 %.1f도
            습도 %d%%
            강수 %s
            바람 %s
            산책수준 %s
            """
            .formatted(
                    dog.getDogBreed(),
                    dog.getDogSize(),
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.isRaining() ? "비" : "강수없음",
                    weather.isWindy() ? "강풍" : "바람약함",
                    weather.getWalkLevel().equals("ERROR") ? "정보없음" : weather.getWalkLevel()
            );
  }

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
}
