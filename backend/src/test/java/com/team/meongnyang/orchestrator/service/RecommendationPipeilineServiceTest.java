package com.team.meongnyang.orchestrator.service;

import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.rag.service.RagService;
import com.team.meongnyang.recommendation.service.*;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherGridPoint;
import com.team.meongnyang.recommendation.cache.WeatherCacheService;
import com.team.meongnyang.recommendation.weather.service.WeatherGridConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPipeilineServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PROMPT = "recommendation prompt";
    private static final String CACHE_KEY = "gemini:test:key";
    private static final String GEMINI_RESPONSE = "generated recommendation";

    @Mock
    private RecommendationUserReader orchUserService;
    @Mock
    private RecommnedationPetReader recommnedationPetReader;
    @Mock
    private WeatherCacheService weatherService;
    @Mock
    private CandidatePlaceService candidatePlaceService;
    @Mock
    private RagService ragService;
    @Mock
    private PlaceScoringService placeScoringService;
    @Mock
    private RecommendationPromptService recommendationPromptService;
    @Mock
    private GeminiCacheService geminiCacheService;
    @Mock
    private GeminiRecommendationService geminiRecommendationService;
    @Mock
    private WeatherGridConverter weatherGridConverter;
    @Mock
    private AiLogService aiLogservice;

    @InjectMocks
    private RecommendationPipelineService recommendationPipeilineService;

    /**
     * 전체 추천 흐름이 정상적으로 끝까지 수행되는지
     * recommendationOrchestratorService 테스트
     *
     * Mock : 가짜 객체
     * Mockito = 가짜 객체 만들고 조작하는 도구
     * @InjectMocks : Mock 주입
     */
    @Test
    @DisplayName("전체 추천 성공 흐름")
    void recommendForCurrentUser_success() {
        // 테스트 변수(객체)
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        String ragContext = "rag context";

        /*
         * 테스트 전략
         * 1. given : 테스트를 위한 준비
         * 2. when : 테스트를 위한 실행
         * 3. then : 테스트를 위한 검증
         */

        // when()을 통해 mock 객체의 동작을 미리 세팅함
        // ex) orchUserService.getCurrentUserByEmail(EMAIL) = user 리턴

        // given : 사용자와 대표 반려견 조회
        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);

        // given : 현재 위치를 날씨 격자로 변환 후 날씨 조회
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);

        // given : 추천 후보 장소 조회 및 RAG 문맥 검색
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);

        // given : 후보 장소 점수 계산 후 추천 프롬프트 생성
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext)).thenReturn(PROMPT);

        // given : Gemini 캐시 조회(MISS) 후 응답 생성
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);

        // when : 추천 로직 실행
        String result = recommendationPipeilineService.recommendForCurrentUser(EMAIL);

        // then: Gemini 응답이 최종 결과로 반환된다
        assertThat(result).isEqualTo(GEMINI_RESPONSE);

        // then: 메서드 호출 순서 검증, 반드시 이 순서대로여야 함
        InOrder inOrder = inOrder(
                orchUserService,
                recommnedationPetReader,
                weatherGridConverter,
                weatherService,
                candidatePlaceService,
                ragService,
                placeScoringService,
                recommendationPromptService,
                geminiCacheService,
                geminiRecommendationService
        );

        // then: 추천 흐름이 설계된 순서대로 호출된다
        inOrder.verify(orchUserService).getCurrentUserByEmail(EMAIL);
        inOrder.verify(recommnedationPetReader).getPrimaryPet(user);
        inOrder.verify(weatherGridConverter).convertToGrid(37.27, 127.01);
        inOrder.verify(weatherService).getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy());
        inOrder.verify(candidatePlaceService).getInitialCandidates(user, pet, weatherContext, 37.27, 127.01);
        inOrder.verify(ragService).searchContext(pet, weatherContext);
        inOrder.verify(placeScoringService).scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01);
        inOrder.verify(recommendationPromptService).buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext);
        inOrder.verify(geminiCacheService).generateKey(PROMPT);
        inOrder.verify(geminiCacheService).get(CACHE_KEY);
        inOrder.verify(geminiRecommendationService).generateRecommendation(PROMPT);
        inOrder.verify(geminiCacheService).save(CACHE_KEY, GEMINI_RESPONSE);

        // then: Gemini 응답은 캐시에 저장되고 AI 로그가 남는다

        // then : Gemini 결과를 중복 저장하지 않고 한 번만 저장했는지 검증
        verify(geminiCacheService, times(1)).save(CACHE_KEY, GEMINI_RESPONSE);


        // eq : 특정 값과 일치하는지 검증하는 matcher
        verify(aiLogservice, times(1)).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                argThat(this::containsTopPlaceSummary),
                eq(ragContext),
                eq(GEMINI_RESPONSE),
                eq(false),
                eq(false),
                longThat(latency -> latency >= 0L)
        );
    }

    /**
     * 유저 생성 테스트용 fixture
     * @return
     */
    private User fixtureUser() {
        return User.builder()
                .userId(1L)
                .email(EMAIL)
                .password("encoded-password")
                .nickname("tester")
                .build();
    }

    /**
     * 펫 생성 테스트용 fixture
     * @param user
     * @return
     */
    private Pet fixturePet(User user) {
        return Pet.builder()
                .petId(10L)
                .user(user)
                .petName("dog")
                .petBreed("breed")
                .petSize(Pet.PetSize.SMALL)
                .petAge(4)
                .petWeight(new BigDecimal("3.20"))
                .petActivity(Pet.PetActivity.NORMAL)
                .personality("calm")
                .preferredPlace("cafe")
                .isRepresentative(true)
                .build();
    }

    /**
     * 좌표 생성 테스트용 fixture
     * @return
     */
    private WeatherGridPoint fixtureGridPoint() {
        return new WeatherGridPoint(60, 121);
    }

    /**
     * 날씨 생성 테스트용 fixture
     * @return
     */
    private WeatherContext fixtureWeatherContext() {
        return WeatherContext.builder()
                .temperature(21.5)
                .humidity(60)
                .precipitationType("NONE")
                .rainfall(0.0)
                .windSpeed(1.2)
                .raining(false)
                .cold(false)
                .hot(false)
                .windy(false)
                .walkLevel("GOOD")
                .build();
    }

    /**
     * 장소 생성 테스트용 fixture
     * @return
     */
    private List<Place> fixtureCandidates() {
        return List.of(
                fixturePlace(100L, "Alpha Cafe"),
                fixturePlace(200L, "Beta Park")
        );
    }

    /**
     * 후보 장소 생성 테스트용 fixture
     * @return
     */
    private List<ScoredPlace> fixtureRankedPlaces() {
        return List.of(
                ScoredPlace.builder()
                        .place(fixturePlace(100L, "Alpha Cafe"))
                        .totalScore(97.3)
                        .dogFitScore(48.0)
                        .weatherScore(19.0)
                        .placeEnvScore(14.0)
                        .distanceScore(9.0)
                        .historyScore(4.0)
                        .penaltyScore(0.0)
                        .summary("summary one")
                        .reason("reason one")
                        .build(),
                ScoredPlace.builder()
                        .place(fixturePlace(200L, "Beta Park"))
                        .totalScore(88.1)
                        .dogFitScore(43.0)
                        .weatherScore(16.0)
                        .placeEnvScore(13.0)
                        .distanceScore(8.0)
                        .historyScore(3.0)
                        .penaltyScore(0.0)
                        .summary("summary two")
                        .reason("reason two")
                        .build()
        );
    }

    /**
     * 장소 생성 테스트용 fixture
     * @param id
     * @param title
     * @return
     */
    private Place fixturePlace(Long id, String title) {
        return Place.builder()
                .id(id)
                .contentId("content-" + id)
                .isVerified(true)
                .title(title)
                .description(title + " description")
                .address("Suwon")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .rating(4.7)
                .reviewCount(120)
                .imageUrl("https://example.com/" + id)
                .phone("010-0000-0000")
                .tags("indoor,pet,cafe")
                .build();
    }

    /**
     * 후보 장소의 요약 정보가 주어진 summary에 포함되어 있는지 확인하는 테스트용 fixture
     * @param summary
     * @return
     */
    private boolean containsTopPlaceSummary(String summary) {
        return summary != null
                && summary.contains("Alpha Cafe")
                && summary.contains("Beta Park")
                && summary.contains("97.3")
                && summary.contains("88.1");
    }
}
