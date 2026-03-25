package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.cache.WeatherCacheService;
import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.context.service.RecommendationEvidenceContextService;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherGridPoint;
import com.team.meongnyang.recommendation.weather.service.WeatherGridConverter;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPipelineServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PROMPT = "recommendation prompt";
    private static final String CACHE_KEY = "gemini:test:key";
    private static final double USER_LAT = 37.5665;
    private static final double USER_LNG = 126.9780;
    private static final String GEMINI_RESPONSE = """
            - [추천설명]
            알파 카페는 반려동물과 함께 머무르기 편안한 장소입니다.
            실내 중심 동선이라 현재 날씨 부담이 적습니다.
            상위 점수 근거도 균형이 좋아 1순위로 적합합니다.
            베타 파크는 활동성은 좋지만 오늘 조건에서는 비교 우위가 낮습니다.

            - [알림요약]
            알파 카페는 오늘 반려견과 가볍게 들르기 좋아요
            """;
    private static final String CACHED_RESPONSE = """
            - [추천설명]
            알파 카페는 오늘 조건에서 가장 무난한 선택입니다.
            실내 공간 활용성이 좋아 현재 날씨 제약을 덜 받습니다.
            반려동물 동반 편의 정보도 충분합니다.
            다른 후보보다 거리와 편의성 균형이 좋습니다.

            - [알림요약]
            알파 카페는 오늘 반려견과 편하게 가기 좋아요
            """;

    @Mock
    private RecommendationUserReader recommendationUserReader;
    @Mock
    private RecommendationPetReader recommendationPetReader;
    @Mock
    private WeatherCacheService weatherService;
    @Mock
    private CandidatePlaceService candidatePlaceService;
    @Mock
    private PlaceScoringService placeScoringService;
    @Mock
    private RecommendationEvidenceContextService recommendationEvidenceContextService;
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
    private RecommendationPipelineService recommendationPipelineService;

    @Test
    @DisplayName("캐시 미스 시 Gemini 응답을 생성하고 AI 로그를 저장한다")
    void recommendForCurrentUser_success() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        RecommendationEvidenceContext evidenceContext = fixtureEvidenceContext();

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(candidates);
        when(aiLogservice.getRecentRecommendedPlacePenalties(user.getUserId())).thenReturn(Map.of());
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, USER_LAT, USER_LNG, Map.of())).thenReturn(rankedPlaces);
        when(recommendationEvidenceContextService.buildContext(user, pet, weatherContext, rankedPlaces)).thenReturn(evidenceContext);
        when(recommendationPromptService.buildRecommendationPrompt(evidenceContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);
        when(geminiRecommendationService.isFallbackResponse(GEMINI_RESPONSE)).thenReturn(false);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getUserId()).isEqualTo(user.getUserId());
        assertThat(result.getPetId()).isEqualTo(pet.getPetId());
        assertThat(result.getPlace()).isNotNull();
        assertThat(result.getPlace().getTitle()).isEqualTo("Alpha Cafe");
        assertThat(result.getWeatherType()).isEqualTo("SUNNY");
        assertThat(result.getMessage()).isEqualTo("알파 카페는 오늘 반려견과 가볍게 들르기 좋아요");
        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.isFallbackUsed()).isFalse();

        InOrder inOrder = inOrder(
                recommendationUserReader,
                recommendationPetReader,
                weatherGridConverter,
                weatherService,
                candidatePlaceService,
                aiLogservice,
                placeScoringService,
                recommendationEvidenceContextService,
                recommendationPromptService,
                geminiCacheService,
                geminiRecommendationService
        );

        inOrder.verify(recommendationUserReader).getCurrentUserByEmail(EMAIL);
        inOrder.verify(recommendationPetReader).getPrimaryPet(user);
        inOrder.verify(weatherGridConverter).convertToGrid(USER_LAT, USER_LNG);
        inOrder.verify(weatherService).getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy());
        inOrder.verify(candidatePlaceService).getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG);
        inOrder.verify(aiLogservice).getRecentRecommendedPlacePenalties(user.getUserId());
        inOrder.verify(placeScoringService).scorePlaces(candidates, user, pet, weatherContext, USER_LAT, USER_LNG, Map.of());
        inOrder.verify(recommendationEvidenceContextService).buildContext(user, pet, weatherContext, rankedPlaces);
        inOrder.verify(recommendationPromptService).buildRecommendationPrompt(evidenceContext);
        inOrder.verify(geminiCacheService).generateKey(PROMPT);
        inOrder.verify(geminiCacheService).get(CACHE_KEY);
        inOrder.verify(geminiRecommendationService).generateRecommendation(PROMPT);
        inOrder.verify(geminiRecommendationService).isFallbackResponse(GEMINI_RESPONSE);
        inOrder.verify(geminiCacheService).save(CACHE_KEY, GEMINI_RESPONSE);

        verify(aiLogservice, times(1)).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                argThat(this::containsTopPlaceSummary),
                eq(100L),
                eq(evidenceContext.getContextSnapshot()),
                eq(GEMINI_RESPONSE),
                eq(false),
                eq(false),
                longThat(latency -> latency >= 0L)
        );
    }

    @Test
    @DisplayName("캐시 히트 시 Gemini 호출 없이 캐시 응답을 반환한다")
    void recommendForCurrentUser_cacheHit() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        RecommendationEvidenceContext evidenceContext = fixtureEvidenceContext();

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(candidates);
        when(aiLogservice.getRecentRecommendedPlacePenalties(user.getUserId())).thenReturn(Map.of());
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, USER_LAT, USER_LNG, Map.of())).thenReturn(rankedPlaces);
        when(recommendationEvidenceContextService.buildContext(user, pet, weatherContext, rankedPlaces)).thenReturn(evidenceContext);
        when(recommendationPromptService.buildRecommendationPrompt(evidenceContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(CACHED_RESPONSE);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getWeatherType()).isEqualTo("SUNNY");
        assertThat(result.getMessage()).isEqualTo("알파 카페는 오늘 반려견과 편하게 가기 좋아요");
        assertThat(result.isCacheHit()).isTrue();
        assertThat(result.isFallbackUsed()).isFalse();

        verify(geminiRecommendationService, never()).generateRecommendation(PROMPT);
        verify(geminiRecommendationService, never()).isFallbackResponse(any());
        verify(geminiCacheService, never()).save(CACHE_KEY, CACHED_RESPONSE);
        verify(aiLogservice, times(1)).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                argThat(this::containsTopPlaceSummary),
                eq(100L),
                eq(evidenceContext.getContextSnapshot()),
                eq(CACHED_RESPONSE),
                eq(false),
                eq(true),
                eq(0L)
        );
    }

    @Test
    @DisplayName("후보 장소가 없으면 추천 불가 응답을 반환하고 후속 단계를 호출하지 않는다")
    void recommendForCurrentUser_noCandidates() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(List.of());

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getPlace()).isNull();
        assertThat(result.getMessage()).contains("추천 가능한 장소");
        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.isFallbackUsed()).isFalse();

        verify(placeScoringService, never()).scorePlaces(any(), any(), any(), any(), any(Double.class), any(Double.class), any());
        verify(recommendationEvidenceContextService, never()).buildContext(any(), any(), any(), any());
        verify(recommendationPromptService, never()).buildRecommendationPrompt(any());
        verify(geminiCacheService, never()).generateKey(any());
        verify(aiLogservice, never()).save(any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(Boolean.class), any(Long.class));
    }

    @Test
    @DisplayName("비가 아니어도 walkLevel이 CAUTION이면 CLOUDY를 반환한다")
    void recommendForCurrentUser_cautionWeatherType() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureCloudyWeatherContext();
        List<Place> candidates = fixtureCandidates();
        List<ScoredPlace> rankedPlaces = fixtureRankedPlaces();
        RecommendationEvidenceContext evidenceContext = fixtureEvidenceContext();

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(candidates);
        when(aiLogservice.getRecentRecommendedPlacePenalties(user.getUserId())).thenReturn(Map.of());
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, USER_LAT, USER_LNG, Map.of())).thenReturn(rankedPlaces);
        when(recommendationEvidenceContextService.buildContext(user, pet, weatherContext, rankedPlaces)).thenReturn(evidenceContext);
        when(recommendationPromptService.buildRecommendationPrompt(evidenceContext)).thenReturn(PROMPT);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(CACHE_KEY);
        when(geminiCacheService.get(CACHE_KEY)).thenReturn(CACHED_RESPONSE);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getWeatherType()).isEqualTo("CLOUDY");
    }

    private User fixtureUser() {
        return User.builder()
                .userId(1L)
                .email(EMAIL)
                .password("encoded-password")
                .nickname("tester")
                .latitude(USER_LAT)
                .longitude(USER_LNG)
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

    private WeatherContext fixtureCloudyWeatherContext() {
        return WeatherContext.builder()
                .temperature(21.5)
                .humidity(70)
                .precipitationType("NONE")
                .rainfall(0.0)
                .windSpeed(6.2)
                .raining(false)
                .cold(false)
                .hot(false)
                .windy(true)
                .walkLevel("CAUTION")
                .build();
    }

    private RecommendationEvidenceContext fixtureEvidenceContext() {
        return RecommendationEvidenceContext.builder()
                .userProfileSection("사용자 정보")
                .petProfileSection("반려동물 정보")
                .weatherSection("날씨 정보")
                .recommendationDecisionSummary("추천 판단 요약")
                .topPlaceEvidenceSection("상위 장소 근거")
                .supplementalGuidelineSection("추가 지침")
                .contextSnapshot("컨텍스트 스냅샷")
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
