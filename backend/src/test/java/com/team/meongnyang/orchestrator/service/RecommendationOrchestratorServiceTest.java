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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationOrchestratorServiceTest {

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
    private RecommendationOrchestratorService recommendationOrchestratorService;

    @Test
    @DisplayName("전체 추천 성공 흐름")
    void recommendForCurrentUser_success() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        String ragContext = "rag context";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);

        String result = recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        assertThat(result).isEqualTo(GEMINI_RESPONSE);

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

        verify(geminiCacheService, times(1)).save(CACHE_KEY, GEMINI_RESPONSE);
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

    @Test
    @DisplayName("사용자 조회 실패 시 이후 호출 중단")
    void recommendForCurrentUser_userLookupFailure_stopsFlow() {
        RuntimeException exception = new RuntimeException("user lookup failed");
        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenThrow(exception);

        assertThatThrownBy(() -> recommendationOrchestratorService.recommendForCurrentUser(EMAIL))
                .isSameAs(exception);

        verify(orchUserService).getCurrentUserByEmail(EMAIL);
        verifyNoInteractions(
                recommnedationPetReader,
                weatherGridConverter,
                weatherService,
                candidatePlaceService,
                ragService,
                placeScoringService,
                recommendationPromptService,
                geminiCacheService,
                geminiRecommendationService,
                aiLogservice
        );
    }

    @Test
    @DisplayName("반려견 조회 실패 시 이후 호출 중단")
    void recommendForCurrentUser_petLookupFailure_stopsFlow() {
        User user = fixtureUser();
        RuntimeException exception = new RuntimeException("pet lookup failed");

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenThrow(exception);

        assertThatThrownBy(() -> recommendationOrchestratorService.recommendForCurrentUser(EMAIL))
                .isSameAs(exception);

        verify(orchUserService).getCurrentUserByEmail(EMAIL);
        verify(recommnedationPetReader).getPrimaryPet(user);
        verifyNoInteractions(
                weatherGridConverter,
                weatherService,
                candidatePlaceService,
                ragService,
                placeScoringService,
                recommendationPromptService,
                geminiCacheService,
                geminiRecommendationService,
                aiLogservice
        );
    }

    @Test
    @DisplayName("날씨 조회 실패 시 처리 검증")
    void recommendForCurrentUser_weatherLookupFailure_stopsFlow() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        RuntimeException exception = new RuntimeException("weather lookup failed");

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenThrow(exception);

        assertThatThrownBy(() -> recommendationOrchestratorService.recommendForCurrentUser(EMAIL))
                .isSameAs(exception);

        verify(weatherGridConverter).convertToGrid(37.27, 127.01);
        verify(weatherService).getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy());
        verifyNoInteractions(
                candidatePlaceService,
                ragService,
                placeScoringService,
                recommendationPromptService,
                geminiCacheService,
                geminiRecommendationService,
                aiLogservice
        );

        // TODO: 날씨 조회 실패를 fallback 응답으로 전환할지 정책이 확정되면 기대 동작을 보강한다.
    }

    @Test
    @DisplayName("후보 장소 없음")
    void recommendForCurrentUser_noCandidates_returnsDefaultResponse() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        String ragContext = "rag context";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(List.of());
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(List.of(), user, pet, weatherContext, 37.27, 127.01)).thenReturn(List.of());

        String result = recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        assertThat(result).isNotBlank();
        verify(recommendationPromptService, never()).buildRecommendationPrompt(eq(user), eq(pet), eq(weatherContext), eq(List.of()), eq(ragContext));
        verify(geminiCacheService, never()).generateKey(PROMPT);
        verify(geminiRecommendationService, never()).generateRecommendation(PROMPT);
        verify(aiLogservice, never()).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                eq(""),
                eq(ragContext),
                eq(GEMINI_RESPONSE),
                eq(false),
                eq(false),
                eq(0L)
        );
    }

    @Test
    @DisplayName("점수 계산 결과 없음")
    void recommendForCurrentUser_noRankedPlaces_stopsBeforePrompt() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        String ragContext = "rag context";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(List.of());

        String result = recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        assertThat(result).isNotBlank();
        verify(recommendationPromptService, never()).buildRecommendationPrompt(eq(user), eq(pet), eq(weatherContext), eq(List.of()), eq(ragContext));
        verify(geminiCacheService, never()).generateKey(PROMPT);
        verify(geminiRecommendationService, never()).generateRecommendation(PROMPT);
        verify(geminiCacheService, never()).save(CACHE_KEY, GEMINI_RESPONSE);
        verify(aiLogservice, never()).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                argThat(this::containsTopPlaceSummary),
                eq(ragContext),
                eq(GEMINI_RESPONSE),
                eq(false),
                eq(false),
                eq(0L)
        );
    }

    @Test
    @DisplayName("RAG 결과 없음")
    void recommendForCurrentUser_blankRagContext_keepsFlow() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn("");
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, "")).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);

        String result = recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        assertThat(result).isEqualTo(GEMINI_RESPONSE);
        verify(recommendationPromptService).buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, "");
        verify(geminiRecommendationService).generateRecommendation(PROMPT);
    }

    @Test
    @DisplayName("Gemini 응답 실패")
    void recommendForCurrentUser_geminiFailure_returnsFallbackAndSavesFailureLog() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        String ragContext = "rag context";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenThrow(new RuntimeException("gemini failed"));

        String result = recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        assertThat(result).isNotBlank();
        verify(geminiCacheService, never()).save(CACHE_KEY, GEMINI_RESPONSE);
        verify(aiLogservice).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                argThat(this::containsTopPlaceSummary),
                eq(ragContext),
                eq(result),
                eq(true),
                eq(false),
                eq(0L)
        );
    }

    @Test
    @DisplayName("Gemini 캐시 적중")
    void recommendForCurrentUser_cacheHit_returnsCachedResponse() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        String ragContext = "rag context";
        String cachedResponse = "cached recommendation";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(cachedResponse);

        String result = recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        assertThat(result).isEqualTo(cachedResponse);
        verify(geminiRecommendationService, never()).generateRecommendation(PROMPT);
        verify(geminiCacheService, never()).save(CACHE_KEY, cachedResponse);
        verify(aiLogservice).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                argThat(this::containsTopPlaceSummary),
                eq(ragContext),
                eq(cachedResponse),
                eq(false),
                eq(true),
                eq(0L)
        );
    }

    @Test
    @DisplayName("캐시 저장 호출 여부 검증")
    void recommendForCurrentUser_cacheSaveVerified() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        String ragContext = "rag context";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);

        recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

        verify(geminiCacheService, times(1)).save(CACHE_KEY, GEMINI_RESPONSE);
    }

    @Test
    @DisplayName("로그 저장 호출 여부 검증")
    void recommendForCurrentUser_logSaveVerified() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        String ragContext = "rag context";

        when(orchUserService.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommnedationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(37.27, 127.01)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, 37.27, 127.01)).thenReturn(candidates);
        when(ragService.searchContext(pet, weatherContext)).thenReturn(ragContext);
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, 37.27, 127.01)).thenReturn(rankedPlaces);
        when(recommendationPromptService.buildRecommendationPrompt(user, pet, weatherContext, rankedPlaces, ragContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);

        recommendationOrchestratorService.recommendForCurrentUser(EMAIL);

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

    private User fixtureUser() {
        return User.builder()
                .userId(1L)
                .email(EMAIL)
                .password("encoded-password")
                .nickname("tester")
                .build();
    }

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

    private WeatherGridPoint fixtureGridPoint() {
        return new WeatherGridPoint(60, 121);
    }

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

    private List<Place> fixtureCandidates() {
        return List.of(
                fixturePlace(100L, "Alpha Cafe"),
                fixturePlace(200L, "Beta Park")
        );
    }

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

    private boolean containsTopPlaceSummary(String summary) {
        return summary != null
                && summary.contains("Alpha Cafe")
                && summary.contains("Beta Park")
                && summary.contains("97.3")
                && summary.contains("88.1");
    }
}
