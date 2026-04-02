package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.cache.GeminiCacheService;
import com.team.meongnyang.recommendation.cache.RecommendationDedupService;
import com.team.meongnyang.recommendation.cache.RecommendationResultCacheService;
import com.team.meongnyang.recommendation.cache.WeatherCacheService;
import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.context.service.RecommendationEvidenceContextService;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.log.RecommendationBatchTraceContext;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.recommendation.weather.dto.WeatherGridPoint;
import com.team.meongnyang.recommendation.weather.service.WeatherGridConverter;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 추천 생성 파이프라인의 전체 흐름을 담당한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RecommendationPipelineService {

    private static final int LOG_TOP_PLACE_LIMIT = 3;
    private static final String NOTIFICATION_SUMMARY_HEADER = "[알림요약]";
    private static final String DEFAULT_NOTIFICATION_SUMMARY = "오늘 조건에 맞는 산책 장소를 안내해 드릴게요.";
    private static final String DUPLICATE_REQUEST_MESSAGE = "추천을 생성 중입니다. 잠시 후 다시 시도해 주세요.";
    private static final String RECOMMENDATION_ERROR_MESSAGE = "추천 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    private static final String RECOMMENDATION_IN_PROGRESS_CODE = "RECOMMENDATION_IN_PROGRESS";
    private static final String RECOMMENDATION_FAILED_CODE = "RECOMMENDATION_FAILED";

    private final RecommendationUserReader recommendationUserReader;
    private final RecommendationPetReader recommendationPetReader;
    private final WeatherCacheService weatherCacheService;
    private final CandidatePlaceService candidatePlaceService;
    private final PlaceScoringService placeScoringService;
    private final RecommendationEvidenceContextService recommendationEvidenceContextService;
    private final RecommendationPromptService recommendationPromptService;
    private final GeminiCacheService geminiCacheService;
    private final GeminiContextFingerprintService geminiContextFingerprintService;
    private final GeminiRecommendationService geminiRecommendationService;
    private final WeatherGridConverter weatherGridConverter;
    private final AiLogService aiLogService;
    private final RecommendationContextKeyFactory recommendationContextKeyFactory;
    private final RecommendationResultCacheService recommendationResultCacheService;
    private final RecommendationDedupService recommendationDedupService;

    /**
     * 현재 로그인한 사용자의 추천 결과를 생성한다.
     */
    public RecommendationNotificationResult recommendForCurrentUser(String email) {
        User user = recommendationUserReader.getCurrentUserByEmail(email);
        Pet pet = recommendationPetReader.getPrimaryPet(user);

        boolean lockAcquired = recommendationDedupService.tryAcquireUserRequestLock(user.getUserId());
        if (!lockAcquired) {
            log.info("[RecommendationPipeline] duplicate request blocked. userId={}", user.getUserId());
            return inProgressRecommendation(user, pet);
        }

        return recommendForNotification(user, pet);
    }

    public RecommendationNotificationResult recommendForNotification(User user, Pet pet) {
        return recommendForNotification(user, pet, null);
    }

    public RecommendationNotificationResult recommendForNotification(User user, Pet pet, String batchExecutionId) {
        try (RecommendationBatchTraceContext.TraceScope ignored =
                     RecommendationBatchTraceContext.open(batchExecutionId, user.getUserId(), pet.getPetId())) {

            double latitude = user.getLatitude();
            double longitude = user.getLongitude();
            WeatherGridPoint gridPoint = weatherGridConverter.convertToGrid(latitude, longitude);
            WeatherContext weatherContext = weatherCacheService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy());

            List<Place> candidates = candidatePlaceService.getInitialCandidates(
                    user,
                    pet,
                    weatherContext,
                    latitude,
                    longitude
            );

            log.info("[RecommendationPipeline] candidate count={}, topNames={}",
                    candidates.size(),
                    summarizePlaceNames(candidates, LOG_TOP_PLACE_LIMIT));

            if (candidates.isEmpty()) {
                return emptyRecommendation(
                        user,
                        pet,
                        weatherContext,
                        "추천 가능한 장소가 없습니다. 조건을 조금 바꿔 다시 시도해 주세요."
                );
            }

            String recommendationCacheKey = recommendationContextKeyFactory.buildResultKey(
                    user,
                    pet,
                    weatherContext,
                    candidates,
                    recommendationDedupService.getLastRecommendedPlaceId(user.getUserId())
            );

            RecommendationNotificationResult cachedResult = recommendationResultCacheService.get(recommendationCacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }

            Map<Long, AiLogService.RecommendationDiversityPenalty> diversityPenalties = loadDiversityPenalties(user);

            List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                    candidates,
                    user,
                    pet,
                    weatherContext,
                    latitude,
                    longitude,
                    diversityPenalties
            );

            log.info("[RecommendationPipeline] ranked count={}, topScores={}",
                    rankedPlaces.size(),
                    summarizeScoredPlaces(rankedPlaces, LOG_TOP_PLACE_LIMIT));

            if (rankedPlaces.isEmpty()) {
                return emptyRecommendation(user, pet, weatherContext, "추천 가능한 장소가 없습니다.");
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

            String prompt = recommendationPromptService.buildRecommendationPrompt(evidenceContext);
            String recommendedPlaces = buildTopPlaceSummary(rankedPlaces);
            String geminiContextKey = geminiCacheService.generateContextKey(
                    geminiContextFingerprintService.buildFingerprint(weatherContext, pet, rankedPlaces)
            );

            String contextCachedResponse = geminiCacheService.get(geminiContextKey);
            if (contextCachedResponse != null) {
                RecommendationNotificationResult result = buildRecommendationResult(
                        user,
                        pet,
                        weatherContext,
                        topPlace,
                        contextCachedResponse,
                        true,
                        false,
                        geminiContextKey
                );
                recommendationResultCacheService.save(recommendationCacheKey, result);
                if (topPlace != null) {
                    recommendationDedupService.recordRecommendation(user.getUserId(), topPlace.getId());
                }
                return result;
            }

            String promptCacheKey = geminiCacheService.generateKey(prompt);
            String promptCachedResponse = geminiCacheService.get(promptCacheKey);
            if (promptCachedResponse != null) {
                aiLogService.save(
                        user,
                        pet,
                        prompt,
                        recommendedPlaces,
                        topPlace == null ? null : topPlace.getId(),
                        evidenceContext.getContextSnapshot(),
                        promptCachedResponse,
                        false,
                        true,
                        0L
                );

                RecommendationNotificationResult result = buildRecommendationResult(
                        user,
                        pet,
                        weatherContext,
                        topPlace,
                        promptCachedResponse,
                        true,
                        false,
                        promptCacheKey
                );
                recommendationResultCacheService.save(recommendationCacheKey, result);
                if (topPlace != null) {
                    recommendationDedupService.recordRecommendation(user.getUserId(), topPlace.getId());
                }
                return result;
            }

            long startTime = System.currentTimeMillis();
            String geminiMessage = geminiRecommendationService.generateRecommendation(prompt);
            long latencyMs = System.currentTimeMillis() - startTime;
            boolean fallbackUsed = geminiRecommendationService.isFallbackResponse(geminiMessage);

            if (!fallbackUsed) {
                geminiCacheService.save(promptCacheKey, geminiMessage);
                geminiCacheService.saveContext(geminiContextKey, geminiMessage);
            }

            aiLogService.save(
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

            RecommendationNotificationResult result = buildRecommendationResult(
                    user,
                    pet,
                    weatherContext,
                    topPlace,
                    geminiMessage,
                    false,
                    fallbackUsed,
                    fallbackUsed ? null : promptCacheKey
            );
            recommendationResultCacheService.save(recommendationCacheKey, result);
            if (topPlace != null) {
                recommendationDedupService.recordRecommendation(user.getUserId(), topPlace.getId());
            }
            return result;
        } catch (Exception e) {
            log.error("[RecommendationPipeline] recommendation pipeline failed.", e);
            return errorRecommendation(user, pet);
        }
    }

    private RecommendationNotificationResult emptyRecommendation(
            User user,
            Pet pet,
            WeatherContext weatherContext,
            String message
    ) {
        return RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType(resolveWeatherType(weatherContext))
                .weatherWalkLevel(weatherContext == null ? null : weatherContext.getWalkLevel())
                .weatherSummary(buildWeatherSummary(weatherContext))
                .place(null)
                .message(message)
                .fallbackUsed(false)
                .cacheHit(false)
                .error(false)
                .build();
    }

    private RecommendationNotificationResult buildRecommendationResult(
            User user,
            Pet pet,
            WeatherContext weatherContext,
            Place topPlace,
            String aiResponse,
            boolean cacheHit,
            boolean fallbackUsed,
            String cacheKey
    ) {
        String notificationSummary = extractNotificationSummary(aiResponse);
        if (notificationSummary == null || notificationSummary.isBlank()) {
            notificationSummary = DEFAULT_NOTIFICATION_SUMMARY;
        }

        return RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType(resolveWeatherType(weatherContext))
                .weatherWalkLevel(weatherContext == null ? null : weatherContext.getWalkLevel())
                .weatherSummary(buildWeatherSummary(weatherContext))
                .place(topPlace)
                .message(notificationSummary)
                .fallbackUsed(fallbackUsed)
                .cacheHit(cacheHit)
                .error(false)
                .aiResponse(aiResponse)
                .geminiCacheKey(cacheKey)
                .build();
    }

    private RecommendationNotificationResult inProgressRecommendation(User user, Pet pet) {
        return RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("IN_PROGRESS")
                .weatherWalkLevel(null)
                .weatherSummary("")
                .place(null)
                .message(DUPLICATE_REQUEST_MESSAGE)
                .fallbackUsed(false)
                .cacheHit(false)
                .error(true)
                .errorCode(RECOMMENDATION_IN_PROGRESS_CODE)
                .build();
    }

    private RecommendationNotificationResult errorRecommendation(User user, Pet pet) {
        return RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("ERROR")
                .weatherWalkLevel("ERROR")
                .weatherSummary("")
                .place(null)
                .message(RECOMMENDATION_ERROR_MESSAGE)
                .fallbackUsed(false)
                .cacheHit(false)
                .error(true)
                .errorCode(RECOMMENDATION_FAILED_CODE)
                .aiResponse(RECOMMENDATION_ERROR_MESSAGE)
                .geminiCacheKey(null)
                .build();
    }

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

    private String buildTopPlaceSummary(List<ScoredPlace> rankedPlaces) {
        if (rankedPlaces == null || rankedPlaces.isEmpty()) {
            return "추천 장소 없음";
        }

        return rankedPlaces.stream()
                .limit(3)
                .map(scoredPlace -> {
                    Place place = scoredPlace.getPlace();
                    return place.getTitle() + "(" + scoredPlace.getTotalScore() + ")";
                })
                .collect(Collectors.joining(", "));
    }

    private Map<Long, AiLogService.RecommendationDiversityPenalty> loadDiversityPenalties(User user) {
        if (user == null) {
            return Map.of();
        }

        Map<Long, AiLogService.RecommendationDiversityPenalty> penalties =
                aiLogService.getRecentRecommendedPlacePenalties(user.getUserId());
        if (penalties == null || penalties.isEmpty()) {
            return Map.of();
        }

        log.info("[RecommendationPipeline] diversity penalty count={}, userId={}",
                penalties.size(),
                user.getUserId());
        return penalties;
    }

    private String summarizePlaceNames(List<Place> places, int limit) {
        if (places == null || places.isEmpty()) {
            return "[]";
        }

        return places.stream()
                .limit(limit)
                .map(place -> place == null ? "null" : place.getTitle())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String summarizeScoredPlaces(List<ScoredPlace> rankedPlaces, int limit) {
        if (rankedPlaces == null || rankedPlaces.isEmpty()) {
            return "[]";
        }

        return rankedPlaces.stream()
                .limit(limit)
                .map(scoredPlace -> {
                    Place place = scoredPlace.getPlace();
                    String title = place == null ? "null" : place.getTitle();
                    return title + "|" + scoredPlace.getTotalScore() + "|" +
                            RecommendationTextUtils.abbreviate(scoredPlace.getSummary(), 80);
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }

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

        return firstLine.replaceFirst("^[-*\\s]*", "").trim();
    }
}
