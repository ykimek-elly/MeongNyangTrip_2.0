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
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
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
 * 추천 생성 전체 흐름을 담당하는 핵심 파이프라인 서비스
 * 사용자/반려동물 조회 → 날씨 조회 → 후보 장소 조회 → 점수 계산 → AI 추천 생성 → 캐시 및 로그 저장까지
 * 추천 시스템의 end-to-end 처리를 수행한다.
 *
 * 중복 요청 방지, 캐시 활용, AI 호출 최적화, 실패 처리까지 포함하여
 * 안정적인 추천 결과 생성과 성능 최적화를 동시에 담당한다.
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
    private final RecommendationAiResponseParser aiResponseParser;

    /**
     * 현재 로그인한 사용자 기준으로 추천을 생성한다.
     * 중복 요청을 방지하기 위해 사용자 단위 락을 먼저 획득한 후 파이프라인을 실행한다.
     */
    public RecommendationNotificationResult recommendForCurrentUser(String email) {
        User user = recommendationUserReader.getCurrentUserByEmail(email);
        Pet pet = recommendationPetReader.getPrimaryPet(user);

        boolean lockAcquired = recommendationDedupService.tryAcquireUserRequestLock(user.getUserId());
        if (!lockAcquired) {
            log.warn("[추천 파이프라인] 중복 요청 차단 userId={}, petId={}",
                    user.getUserId(),
                    pet.getPetId());
            return inProgressRecommendation(user, pet);
        }

        try {
            return recommendForNotification(user, pet);
        } finally {
            recommendationDedupService.releaseUserRequestLock(user.getUserId());
        }
    }

    /**
     * 사용자와 반려동물을 기준으로 추천을 생성한다.
     * 외부에서 호출되는 기본 진입 메서드이다.
     */
    public RecommendationNotificationResult recommendForNotification(User user, Pet pet) {
        return recommendForNotification(user, pet, null);
    }

    /**
     * 추천 파이프라인의 실제 실행 메서드
     * 전체 추천 흐름(날씨 → 후보 → 점수 → AI → 캐시)을 순차적으로 수행한다.
     */
    public RecommendationNotificationResult recommendForNotification(User user, Pet pet, String batchExecutionId) {
        try (RecommendationBatchTraceContext.TraceScope ignored =
                     RecommendationBatchTraceContext.open(batchExecutionId, user.getUserId(), pet.getPetId())) {

            double latitude = user.getLatitude();
            double longitude = user.getLongitude();
            log.info("[추천 파이프라인] 사용자 로드 userId={}, petId={}, batchExecutionId={}, latitude={}, longitude={}",
                    user.getUserId(),
                    pet.getPetId(),
                    batchExecutionId,
                    latitude,
                    longitude);

            if (!isCoordinateInitialized(latitude, longitude)) {
                log.error("[에러] 좌표 정보 이상 userId={}, petId={}, batchExecutionId={}, latitude={}, longitude={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId,
                        latitude,
                        longitude);
                throw new CoordinateInvalidException(latitude, longitude);
            }
            WeatherGridPoint gridPoint = weatherGridConverter.convertToGrid(latitude, longitude);
            log.info("[날씨 조회] 격자 변환 완료 userId={}, petId={}, batchExecutionId={}, nx={}, ny={}",
                    user.getUserId(),
                    pet.getPetId(),
                    batchExecutionId,
                    gridPoint.getNx(),
                    gridPoint.getNy());
            WeatherContext weatherContext = weatherCacheService.getOrLoadWeather(gridPoint.getNx(), gridPoint.getNy());
            if (isWeatherContextUnavailable(weatherContext)) {
                log.error("[에러] 날씨 정보 조회 실패 userId={}, petId={}, batchExecutionId={}, walkLevel={}, precipitationType={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId,
                        weatherContext == null ? null : weatherContext.getWalkLevel(),
                        weatherContext == null ? null : weatherContext.getPrecipitationType());
                return errorRecommendation(user, pet, weatherContext);
            }

            List<Place> candidates = candidatePlaceService.getInitialCandidates(
                    user,
                    pet,
                    weatherContext,
                    latitude,
                    longitude
            );

            log.info("[장소 후보] 후보 조회 완료 userId={}, petId={}, batchExecutionId={}, count={}, topPlaces={}",
                    user.getUserId(),
                    pet.getPetId(),
                    batchExecutionId,
                    candidates.size(),
                    summarizePlaceNames(candidates, LOG_TOP_PLACE_LIMIT));

            if (candidates.isEmpty()) {
                log.warn("[추천 파이프라인] 추천 후보 없음 userId={}, petId={}, batchExecutionId={}, walkLevel={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId,
                        weatherContext == null ? null : weatherContext.getWalkLevel());
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
                log.info("[캐시] 추천 결과 재사용 userId={}, petId={}, batchExecutionId={}, placeId={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId,
                        cachedResult.getPlace() == null ? null : cachedResult.getPlace().getId());
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

            log.info("[장소 점수] 상위 결과 userId={}, petId={}, batchExecutionId={}, count={}, topPlaces={}",
                    user.getUserId(),
                    pet.getPetId(),
                    batchExecutionId,
                    rankedPlaces.size(),
                    summarizeScoredPlaces(rankedPlaces, LOG_TOP_PLACE_LIMIT));

            if (rankedPlaces.isEmpty()) {
                log.warn("[추천 파이프라인] 점수 결과 없음 userId={}, petId={}, batchExecutionId={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId);
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
                log.info("[캐시] AI 응답 재사용 userId={}, petId={}, batchExecutionId={}, cacheType={}, placeId={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId,
                        "context",
                        topPlace == null ? null : topPlace.getId());
                aiLogService.save(
                        user,
                        pet,
                        prompt,
                        recommendedPlaces,
                        topPlace == null ? null : topPlace.getId(),
                        evidenceContext.getContextSnapshot(),
                        contextCachedResponse,
                        false,
                        true,
                        0L
                );
                return cacheAndRecordResult(
                        user,
                        topPlace,
                        recommendationCacheKey,
                        buildRecommendationResult(
                                user,
                                pet,
                                weatherContext,
                                topPlace,
                                contextCachedResponse,
                                true,
                                false,
                                geminiContextKey
                        )
                );
            }

            String promptCacheKey = geminiCacheService.generateKey(prompt);
            String promptCachedResponse = geminiCacheService.get(promptCacheKey);
            if (promptCachedResponse != null) {
                log.info("[캐시] AI 응답 재사용 userId={}, petId={}, batchExecutionId={}, cacheType={}, placeId={}",
                        user.getUserId(),
                        pet.getPetId(),
                        batchExecutionId,
                        "prompt",
                        topPlace == null ? null : topPlace.getId());
                aiLogService.save(
                        user,
                        pet,
                        prompt,
                        recommendedPlaces,
                        topPlace == null ? null : topPlace.getId(),
                        evidenceContext.getContextSnapshot(),
                        promptCachedResponse,
                        false,
                        false,
                        0L
                );

                return cacheAndRecordResult(
                        user,
                        topPlace,
                        recommendationCacheKey,
                        buildRecommendationResult(
                                user,
                                pet,
                                weatherContext,
                                topPlace,
                                promptCachedResponse,
                                true,
                                false,
                                promptCacheKey
                        )
                );
            }

            long startTime = System.currentTimeMillis();
            String geminiMessage = geminiRecommendationService.generateRecommendation(prompt);
            long latencyMs = System.currentTimeMillis() - startTime;
            boolean fallbackUsed = geminiRecommendationService.isFallbackResponse(geminiMessage);
            log.info("[AI 호출] 추천 문장 생성 완료 userId={}, petId={}, batchExecutionId={}, length={}, latencyMs={}, fallbackUsed={}",
                    user.getUserId(),
                    pet.getPetId(),
                    batchExecutionId,
                    geminiMessage == null ? 0 : geminiMessage.length(),
                    latencyMs,
                    fallbackUsed);

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

            return cacheAndRecordResult(
                    user,
                    topPlace,
                    recommendationCacheKey,
                    buildRecommendationResult(
                            user,
                            pet,
                            weatherContext,
                            topPlace,
                            geminiMessage,
                            false,
                            fallbackUsed,
                            fallbackUsed ? null : promptCacheKey
                    )
            );
        } catch (CoordinateInvalidException e) {
            throw e;
        } catch (Exception e) {
            log.error("[에러] 추천 실패 userId={}, petId={}, batchExecutionId={}, reason={}",
                    user == null ? null : user.getUserId(),
                    pet == null ? null : pet.getPetId(),
                    batchExecutionId,
                    e.getMessage(),
                    e);
            return errorRecommendation(user, pet);
        }
    }

    /**
     * 캐시에 결과를 저장하고, 추천 결과를 기록한다.
     */
    private RecommendationNotificationResult cacheAndRecordResult(
            User user,
            Place topPlace,
            String recommendationCacheKey,
            RecommendationNotificationResult result
    ) {
        recommendationResultCacheService.save(recommendationCacheKey, result);
        if (topPlace != null) {
            recommendationDedupService.recordRecommendation(user.getUserId(), topPlace.getId());
        }
        return result;
    }


    /**
     * 추천 가능한 장소가 없을 경우 기본 응답을 생성한다.
     */
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
                .recommendationDescription(message)
                .fallbackUsed(false)
                .cacheHit(false)
                .error(false)
                .build();
    }

    /**
     * 최종 추천 결과 객체를 생성한다.
     * AI 응답을 기반으로 알림용 메시지와 메타 정보를 구성한다.
     */
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
        String notificationSummary = aiResponseParser.extractNotificationSummary(aiResponse);
        if (notificationSummary == null || notificationSummary.isBlank()) {
            notificationSummary = DEFAULT_NOTIFICATION_SUMMARY;
        }
        String recommendationDescription = aiResponseParser.extractRecommendationDescription(aiResponse);
        if (recommendationDescription == null || recommendationDescription.isBlank()) {
            recommendationDescription = aiResponseParser.defaultDescription(notificationSummary);
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
                .recommendationDescription(recommendationDescription)
                .fallbackUsed(fallbackUsed)
                .cacheHit(cacheHit)
                .error(false)
                .aiResponse(aiResponse)
                .geminiCacheKey(cacheKey)
                .build();
    }

    /**
     * 이미 추천이 진행 중인 경우 반환하는 응답을 생성한다.
     */
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
                .recommendationDescription(DUPLICATE_REQUEST_MESSAGE)
                .fallbackUsed(false)
                .cacheHit(false)
                .error(true)
                .errorCode(RECOMMENDATION_IN_PROGRESS_CODE)
                .build();
    }

    /**
     * 추천 처리 중 예외 발생 시 반환하는 오류 응답을 생성한다.
     */
    private RecommendationNotificationResult errorRecommendation(User user, Pet pet) {
        return errorRecommendation(user, pet, null);
    }

    private RecommendationNotificationResult errorRecommendation(User user, Pet pet, WeatherContext weatherContext) {
        return RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("ERROR")
                .weatherWalkLevel(weatherContext == null ? "ERROR" : weatherContext.getWalkLevel())
                .weatherSummary(buildWeatherSummary(weatherContext))
                .place(null)
                .message(RECOMMENDATION_ERROR_MESSAGE)
                .recommendationDescription(RECOMMENDATION_ERROR_MESSAGE)
                .fallbackUsed(false)
                .cacheHit(false)
                .error(true)
                .errorCode(RECOMMENDATION_FAILED_CODE)
                .aiResponse(RECOMMENDATION_ERROR_MESSAGE)
                .geminiCacheKey(null)
                .build();
    }

    /**
     * 날씨 정보를 기반으로 알림용 날씨 타입을 결정한다.
     */
    private String resolveWeatherType(WeatherContext weatherContext) {
        if (weatherContext == null) {
            return "SUNNY";
        }
        if ("ERROR".equalsIgnoreCase(weatherContext.getWalkLevel())
                || "ERROR".equalsIgnoreCase(weatherContext.getPrecipitationType())) {
            return "ERROR";
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
     * 날씨 정보를 문자열 형태로 요약한다.
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
     * 상위 추천 장소를 문자열로 요약한다.
     */
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

    /**
     * 최근 추천 이력을 기반으로 다양성 패널티 정보를 조회한다.
     */
    private Map<Long, AiLogService.RecommendationDiversityPenalty> loadDiversityPenalties(User user) {
        if (user == null) {
            return Map.of();
        }

        Map<Long, AiLogService.RecommendationDiversityPenalty> penalties =
                aiLogService.getRecentRecommendedPlacePenalties(user.getUserId());
        if (penalties == null || penalties.isEmpty()) {
            return Map.of();
        }

        log.info("[추천 파이프라인] 다양성 감점 로드 userId={}, petId={}, batchExecutionId={}, count={}",
                user.getUserId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                penalties.size());
        return penalties;
    }

    /**
     * 장소 리스트를 로그용 문자열로 요약한다.
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
     * 점수 계산된 장소 리스트를 로그용 문자열로 요약한다.
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
                    String boosts = summarizeAdjustments(scoredPlace.getAppliedBoosts(), "boost");
                    String penalties = summarizeAdjustments(scoredPlace.getAppliedPenalties(), "penalty");
                    return title + "|" + scoredPlace.getTotalScore() + "|" +
                            boosts + "|" + penalties + "|" +
                            RecommendationTextUtils.abbreviate(scoredPlace.getSummary(), 80);
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String summarizeAdjustments(List<String> adjustments, String label) {
        if (adjustments == null || adjustments.isEmpty()) {
            return label + "=none";
        }

        String summarized = adjustments.stream()
                .limit(3)
                .map(adjustment -> RecommendationTextUtils.abbreviate(adjustment, 28))
                .collect(Collectors.joining(";"));
        return label + "=" + summarized;
    }

    /**
     * AI 응답에서 알림용 요약 문장을 추출한다.
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

        return firstLine.replaceFirst("^[-*\\s]*", "").trim();
    }

    /**
     * 사용자 좌표가 정상적으로 설정되어 있는지 검증한다.
     */
    private boolean isCoordinateInitialized(double latitude, double longitude) {
        if (Double.compare(latitude, 0.0d) == 0 && Double.compare(longitude, 0.0d) == 0) {
            return false;
        }
        return latitude >= -90.0d && latitude <= 90.0d
                && longitude >= -180.0d && longitude <= 180.0d;
    }

    private boolean isWeatherContextUnavailable(WeatherContext weatherContext) {
        if (weatherContext == null) {
            return true;
        }
        return "ERROR".equalsIgnoreCase(weatherContext.getWalkLevel())
                || "ERROR".equalsIgnoreCase(weatherContext.getPrecipitationType());
    }
}
