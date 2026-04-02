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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPipelineServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PROMPT = "recommendation prompt";
    private static final String PROMPT_CACHE_KEY = "gemini:prompt:key";
    private static final String CONTEXT_CACHE_KEY = "gemini:context:key";
    private static final String RECOMMENDATION_CACHE_KEY = "recommendation:result:key";
    private static final double USER_LAT = 37.5665;
    private static final double USER_LNG = 126.9780;
    private static final String GEMINI_RESPONSE = """
            [추천설명]
            Alpha Cafe는 현재 조건과 반려동물 특성을 고려했을 때 가장 무난한 선택입니다.

            [알림요약]
            Alpha Cafe가 오늘 방문하기 좋습니다.
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
    @DisplayName("Gemini 캐시가 없으면 응답을 생성하고 추천 결과를 캐시한다")
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
        when(geminiContextFingerprintService.buildFingerprint(weatherContext, pet, rankedPlaces)).thenReturn("context-fingerprint");
        when(geminiCacheService.generateContextKey("context-fingerprint")).thenReturn(CONTEXT_CACHE_KEY);
        when(geminiCacheService.get(CONTEXT_CACHE_KEY)).thenReturn(null);
        when(geminiCacheService.generateKey(PROMPT)).thenReturn(PROMPT_CACHE_KEY);
        when(geminiCacheService.get(PROMPT_CACHE_KEY)).thenReturn(null);
        when(geminiRecommendationService.generateRecommendation(PROMPT)).thenReturn(GEMINI_RESPONSE);
        when(geminiRecommendationService.isFallbackResponse(GEMINI_RESPONSE)).thenReturn(false);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getUserId()).isEqualTo(user.getUserId());
        assertThat(result.getPetId()).isEqualTo(pet.getPetId());
        assertThat(result.getPlace()).isNotNull();
        assertThat(result.getPlace().getTitle()).isEqualTo("Alpha Cafe");
        assertThat(result.getWeatherType()).isEqualTo("SUNNY");
        assertThat(result.getMessage()).isEqualTo("Alpha Cafe가 오늘 방문하기 좋습니다.");
        assertThat(result.isCacheHit()).isFalse();
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.isError()).isFalse();

        verify(geminiCacheService, times(1)).save(PROMPT_CACHE_KEY, GEMINI_RESPONSE);
        verify(geminiCacheService, times(1)).saveContext(CONTEXT_CACHE_KEY, GEMINI_RESPONSE);
        verify(recommendationResultCacheService, times(1))
                .save(eq(RECOMMENDATION_CACHE_KEY), any(RecommendationNotificationResult.class));
        verify(recommendationDedupService, times(1)).recordRecommendation(user.getUserId(), 100L);
    }

    @Test
    @DisplayName("추천 결과 캐시가 있으면 점수 계산과 Gemini 호출 없이 바로 반환한다")
    void recommendForCurrentUser_recommendationCacheHit() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();
        List<Place> candidates = fixtureCandidates();
        RecommendationNotificationResult cachedResult = RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("SUNNY")
                .place(fixturePlace(100L, "Alpha Cafe"))
                .message("cached message")
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
        when(recommendationResultCacheService.get(RECOMMENDATION_CACHE_KEY)).thenReturn(cachedResult);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result).isSameAs(cachedResult);
        verify(placeScoringService, never()).scorePlaces(any(), any(), any(), any(), any(Double.class), any(Double.class), any());
        verify(geminiRecommendationService, never()).generateRecommendation(any());
    }

    @Test
    @DisplayName("후보 장소가 없으면 즉시 빈 추천 결과를 반환한다")
    void recommendForCurrentUser_noCandidates() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        WeatherGridPoint gridPoint = fixtureGridPoint();
        WeatherContext weatherContext = fixtureWeatherContext();

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(recommendationDedupService.tryAcquireUserRequestLock(user.getUserId())).thenReturn(true);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenReturn(gridPoint);
        when(weatherCacheService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy())).thenReturn(weatherContext);
        when(candidatePlaceService.getInitialCandidates(user, pet, weatherContext, USER_LAT, USER_LNG)).thenReturn(List.of());

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getPlace()).isNull();
        assertThat(result.getMessage()).contains("추천 가능한 장소");
        verify(placeScoringService, never()).scorePlaces(any(), any(), any(), any(), any(Double.class), any(Double.class), any());
        verify(geminiCacheService, never()).generateKey(any());
    }

    @Test
    @DisplayName("주의 단계 날씨면 CLOUDY 타입을 반환한다")
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
        when(geminiContextFingerprintService.buildFingerprint(weatherContext, pet, rankedPlaces)).thenReturn("context-fingerprint");
        when(geminiCacheService.generateContextKey("context-fingerprint")).thenReturn(CONTEXT_CACHE_KEY);
        when(geminiCacheService.get(CONTEXT_CACHE_KEY)).thenReturn(GEMINI_RESPONSE);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.getWeatherType()).isEqualTo("CLOUDY");
    }

    @Test
    @DisplayName("동시 요청 락을 얻지 못하면 안내 메시지와 오류 코드를 반환한다")
    void recommendForCurrentUser_duplicateRequestBlocked() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(recommendationDedupService.tryAcquireUserRequestLock(user.getUserId())).thenReturn(false);

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.isError()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo("RECOMMENDATION_IN_PROGRESS");
        assertThat(result.getWeatherType()).isEqualTo("IN_PROGRESS");
        verify(weatherGridConverter, never()).convertToGrid(any(Double.class), any(Double.class));
    }

    @Test
    @DisplayName("파이프라인 예외가 발생하면 오류 응답을 숨기지 않고 반환한다")
    void recommendForCurrentUser_pipelineExceptionReturnsError() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);

        when(recommendationUserReader.getCurrentUserByEmail(EMAIL)).thenReturn(user);
        when(recommendationPetReader.getPrimaryPet(user)).thenReturn(pet);
        when(recommendationDedupService.tryAcquireUserRequestLock(user.getUserId())).thenReturn(true);
        when(weatherGridConverter.convertToGrid(USER_LAT, USER_LNG)).thenThrow(new IllegalStateException("boom"));

        RecommendationNotificationResult result = recommendationPipelineService.recommendForCurrentUser(EMAIL);

        assertThat(result.isError()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo("RECOMMENDATION_FAILED");
        assertThat(result.getWeatherType()).isEqualTo("ERROR");
        assertThat(result.getWeatherWalkLevel()).isEqualTo("ERROR");
        assertThat(result.getMessage()).contains("오류");
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
}
