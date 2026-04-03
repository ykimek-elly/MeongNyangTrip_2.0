package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.cache.RecommendationDedupService;
import com.team.meongnyang.recommendation.cache.RecommendationResultCacheService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPipelineLockTest {

    private static final String EMAIL = "user@example.com";
    private static final String PROMPT = "recommendation prompt";
    private static final String PROMPT_CACHE_KEY = "gemini:prompt:key";
    private static final String CONTEXT_CACHE_KEY = "gemini:context:key";
    private static final String RECOMMENDATION_CACHE_KEY = "recommendation:result:key";
    private static final double USER_LAT = 37.5665;
    private static final double USER_LNG = 126.9780;
    private static final String GEMINI_RESPONSE = """
            [추천설명]
            Alpha Cafe는 산책 동선을 무리 없이 이어가기 좋고 반려견과 함께 머물 여유가 있다.

            [알림요약]
            Alpha Cafe는 바람 부담이 적어 짧은 외출 동선에 맞다.
            """;

    @Mock
    private RecommendationUserReader recommendationUserReader;
    @Mock
    private RecommendationPetReader recommendationPetReader;
    @Mock
    private WeatherCacheService weatherCacheService;
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
    private GeminiContextFingerprintService geminiContextFingerprintService;
    @Mock
    private GeminiRecommendationService geminiRecommendationService;
    @Mock
    private WeatherGridConverter weatherGridConverter;
    @Mock
    private AiLogService aiLogService;
    @Mock
    private RecommendationContextKeyFactory recommendationContextKeyFactory;
    @Mock
    private RecommendationResultCacheService recommendationResultCacheService;
    @Mock
    private RecommendationDedupService recommendationDedupService;

    @InjectMocks
    private RecommendationPipelineService recommendationPipelineService;

    @Test
    @DisplayName("현재 사용자 추천이 성공하면 요청 락을 해제한다")
    void recommendForCurrentUser_releasesLockAfterSuccess() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = new WeatherGridPoint(60, 121);
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = List.of(fixturePlace(100L, "Alpha Cafe"));
        List<ScoredPlace> rankedPlaces = List.of(
                ScoredPlace.builder()
                        .place(fixturePlace(100L, "Alpha Cafe"))
                        .totalScore(97.3)
                        .summary("summary one")
                        .reason("reason one")
                        .build()
        );
        RecommendationEvidenceContext evidenceContext = RecommendationEvidenceContext.builder()
                .userProfileSection("사용자 정보")
                .petProfileSection("반려동물 정보")
                .weatherSection("날씨 정보")
                .recommendationDecisionSummary("추천 판단 요약")
                .explanationFocusSection("설명 필수 근거")
                .topPlaceEvidenceSection("상위 장소 근거")
                .supplementalGuidelineSection("추가 지침")
                .contextSnapshot("컨텍스트 스냅샷")
                .build();

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(recommendationDedupService.tryAcquireUserRequestLock(user.getUserId())).thenReturn(true);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherCacheService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(candidates);
        when(recommendationDedupService.getLastRecommendedPlaceId(user.getUserId())).thenReturn(null);
        when(recommendationContextKeyFactory.buildResultKey(user, pet, weatherContext, candidates, null))
                .thenReturn(RECOMMENDATION_CACHE_KEY);
        when(recommendationResultCacheService.get(RECOMMENDATION_CACHE_KEY)).thenReturn(null);
        when(aiLogService.getRecentRecommendedPlacePenalties(user.getUserId())).thenReturn(Map.of());
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, USER_LAT, USER_LNG, Map.of()))
                .thenReturn(rankedPlaces);
        when(recommendationEvidenceContextService.buildContext(user, pet, weatherContext, rankedPlaces))
                .thenReturn(evidenceContext);
        when(recommendationPromptService.buildRecommendationPrompt(evidenceContext)).thenReturn(PROMPT);
        when(geminiContextFingerprintService.buildFingerprint(weatherContext, pet, rankedPlaces))
                .thenReturn("context-fingerprint");
        when(geminiCacheService.generateContextKey("context-fingerprint")).thenReturn(CONTEXT_CACHE_KEY);
        when(geminiCacheService.get(CONTEXT_CACHE_KEY)).thenReturn(null);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(PROMPT_CACHE_KEY);
        when(geminiCacheService.get(PROMPT_CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);
        when(geminiRecommendationService.isFallbackResponse(GEMINI_RESPONSE)).thenReturn(false);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.isError()).isFalse();
        verify(recommendationDedupService).releaseUserRequestLock(user.getUserId());
    }

    @Test
    @DisplayName("중복 요청으로 락을 얻지 못하면 해제를 시도하지 않는다")
    void recommendForCurrentUser_doesNotReleaseWhenLockNotAcquired() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(recommendationDedupService.tryAcquireUserRequestLock(user.getUserId())).thenReturn(false);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.isError()).isTrue();
        verify(recommendationDedupService, never()).releaseUserRequestLock(any());
    }

    @Test
    @DisplayName("날씨 조회가 fallback ERROR면 추천을 중단하고 에러 응답을 반환한다")
    void recommendForNotification_returnsErrorWhenWeatherUnavailable() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = new WeatherGridPoint(60, 121);
        WeatherContext errorWeather = WeatherContext.builder()
                .temperature(0.0)
                .humidity(0)
                .precipitationType("ERROR")
                .rainfall(0.0)
                .windSpeed(0.0)
                .raining(false)
                .cold(false)
                .hot(false)
                .windy(false)
                .walkLevel("ERROR")
                .build();

        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherCacheService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(errorWeather);

        RecommendationNotificationResult result =
                recommendationPipelineService.recommendForNotification(user, pet, "batch-1");

        assertThat(result.isError()).isTrue();
        assertThat(result.getWeatherType()).isEqualTo("ERROR");
        assertThat(result.getWeatherWalkLevel()).isEqualTo("ERROR");
        verify(candidatePlaceService, never()).getInitialCandidates(any(), any(), any(), any(Double.class), any(Double.class));
    }

    @Test
    @DisplayName("context cache hit도 AI 로그를 cacheHit=true로 남긴다")
    void recommendForNotification_logsWhenContextCacheHits() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = new WeatherGridPoint(60, 121);
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = List.of(fixturePlace(100L, "Alpha Cafe"));
        List<ScoredPlace> rankedPlaces = List.of(
                ScoredPlace.builder()
                        .place(fixturePlace(100L, "Alpha Cafe"))
                        .totalScore(97.3)
                        .summary("summary one")
                        .reason("reason one")
                        .build()
        );
        RecommendationEvidenceContext evidenceContext = RecommendationEvidenceContext.builder()
                .userProfileSection("사용자 정보")
                .petProfileSection("반려동물 정보")
                .weatherSection("날씨 정보")
                .recommendationDecisionSummary("추천 판단 요약")
                .explanationFocusSection("설명 필수 근거")
                .topPlaceEvidenceSection("상위 장소 근거")
                .supplementalGuidelineSection("추가 지침")
                .contextSnapshot("컨텍스트 스냅샷")
                .build();

        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherCacheService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(candidates);
        when(recommendationDedupService.getLastRecommendedPlaceId(user.getUserId())).thenReturn(null);
        when(recommendationContextKeyFactory.buildResultKey(user, pet, weatherContext, candidates, null))
                .thenReturn(RECOMMENDATION_CACHE_KEY);
        when(recommendationResultCacheService.get(RECOMMENDATION_CACHE_KEY)).thenReturn(null);
        when(aiLogService.getRecentRecommendedPlacePenalties(user.getUserId())).thenReturn(Map.of());
        when(placeScoringService.scorePlaces(candidates, user, pet, weatherContext, USER_LAT, USER_LNG, Map.of()))
                .thenReturn(rankedPlaces);
        when(recommendationEvidenceContextService.buildContext(user, pet, weatherContext, rankedPlaces))
                .thenReturn(evidenceContext);
        when(recommendationPromptService.buildRecommendationPrompt(evidenceContext)).thenReturn(PROMPT);
        when(geminiContextFingerprintService.buildFingerprint(weatherContext, pet, rankedPlaces))
                .thenReturn("context-fingerprint");
        when(geminiCacheService.generateContextKey("context-fingerprint")).thenReturn(CONTEXT_CACHE_KEY);
        when(geminiCacheService.get(CONTEXT_CACHE_KEY)).thenReturn(GEMINI_RESPONSE);

        RecommendationNotificationResult result =
                recommendationPipelineService.recommendForNotification(user, pet, "batch-1");

        assertThat(result.isError()).isFalse();
        assertThat(result.isCacheHit()).isTrue();
        verify(aiLogService).save(
                eq(user),
                eq(pet),
                eq(PROMPT),
                eq("Alpha Cafe(97.3)"),
                eq(100L),
                eq("컨텍스트 스냅샷"),
                eq(GEMINI_RESPONSE),
                eq(false),
                eq(true),
                eq(0L)
        );
        verify(geminiRecommendationService, never()).generateRecommendation(any());
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
}
