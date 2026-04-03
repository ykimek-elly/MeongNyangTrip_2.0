package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.dto.ScoreBreakdown;
import com.team.meongnyang.recommendation.dto.ScoreDetail;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 후보 장소를 점수화하여 추천 순위를 계산하는 서비스
 * 반려동물 동반 가능 여부, 펫 편의시설, 장소 신뢰도, 블로그 반응, 태그/카테고리 적합도를 기준으로
 * 장소별 총점을 산정하고 내림차순으로 정렬한다.
 *
 * 현재 엔티티 기반 점수 체계는 총 100점 만점으로 구성되며,
 * 펫 출입/정책 적합도 30점, 펫 편의시설 적합도 15점,
 * 장소 품질/신뢰도 20점, 블로그 감성 시그널 15점,
 * 태그/카테고리 적합도 20점을 합산한 뒤 감점을 반영한다.
 *
 * 추가로 최근 추천 이력에 따른 다양성 패널티를 적용해
 * 동일 장소가 반복 추천되지 않도록 보정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlaceScoringService {

    private static final double PET_MAX = 50.0;
    private static final double WEATHER_MAX = 20.0;
    private static final double ENVIRONMENT_MAX = 15.0;
    private static final double MOBILITY_MAX = 10.0;
    private static final double BONUS_MAX = 5.0;
    private static final double TOTAL_MAX = 100.0;
    private static final double POLICY_MAX = 30.0;
    private static final double FACILITY_MAX = 15.0;
    private static final double QUALITY_MAX = 20.0;
    private static final double SENTIMENT_MAX = 15.0;
    private static final double CONTEXT_MAX = 20.0;
    private static final String[] QUIET_INDOOR_KEYWORDS = {"실내", "카페", "조용", "휴식", "아늑", "차분", "편안", "라운지"};
    private static final String[] ACTIVE_OUTDOOR_KEYWORDS = {"공원", "산책로", "운동장", "야외", "실외", "러닝", "잔디", "넓음", "트레킹"};
    private static final String[] WIDE_SPACE_KEYWORDS = {"넓", "넓음", "광장", "잔디", "공원", "산책로", "운동장", "야외"};
    private static final String[] CROWDED_KEYWORDS = {"붐빔", "혼잡", "페스티벌", "행사", "시장", "핫플", "웨이팅", "대기", "복잡"};
    private static final String[] PET_FRIENDLY_KEYWORDS = {"반려견", "반려동물", "동반", "펫", "애견", "펫프렌들리"};
    private static final String[] CONVENIENCE_KEYWORDS = {"주차", "휴식", "배변", "물", "테라스", "그늘", "벤치", "의자"};
    private static final String[] SAFETY_COMFORT_KEYWORDS = {"넓", "쾌적", "조용", "안전", "청결", "편안"};
    private static final String[] OUTDOOR_PLACE_KEYWORDS = {"실외", "야외", "공원", "산책로", "운동장", "테라스", "잔디"};

    private final DistanceCalculator distanceCalculator;

    /**
     * 후보 장소 목록을 점수화하여 추천 순위를 반환한다.
     * 내부적으로 scorePlaces를 호출하는 진입 메서드이다.
     */
    public List<ScoredPlace> scorePlace(List<Place> candidates, User user, Pet pet, WeatherContext weather) {
        return scorePlaces(candidates, user, pet, weather);
    }

    /**
     * 사용자 좌표를 기준으로 후보 장소 전체 점수를 계산한다.
     * 사용자 위도/경도를 읽어 거리 계산에 반영하고,
     * 최종적으로 총점 내림차순으로 정렬된 장소 목록을 반환한다.
     *
     * @param candidates 1차 필터를 통과한 후보 장소 목록
     * @param user 추천 대상 사용자 정보
     * @param pet 추천 기준이 되는 반려동물 정보
     * @param weather 현재 추천에 반영할 날씨 정보
     * @return 총점 내림차순으로 정렬된 장소 점수 목록
     */
    public List<ScoredPlace> scorePlaces(List<Place> candidates, User user, Pet pet, WeatherContext weather) {
        return scorePlaces(
                candidates,
                user,
                pet,
                weather,
                requireUserLatitude(user),
                requireUserLongitude(user),
                Map.of()
        );
    }

    /**
     * 후보 장소 전체를 순회하며 엔티티 기반 점수 계산과 다양성 패널티를 함께 반영한다.
     * 추천 이력이 있는 장소는 감점하고, 후보 수가 적으면 패널티를 완화하거나 무시한다.
     *
     * @param candidates 1차 필터를 통과한 후보 장소 목록
     * @param user 추천 대상 사용자 정보
     * @param pet 추천 기준이 되는 반려동물 정보
     * @param weather 현재 추천에 반영할 날씨 정보
     * @param userLat 거리 계산에 사용할 사용자 위도
     * @param userLng 거리 계산에 사용할 사용자 경도
     * @return 총점 기준 내림차순으로 정렬된 {@link ScoredPlace} 목록
     */
    public List<ScoredPlace> scorePlaces(
            List<Place> candidates,
            User user,
            Pet pet,
            WeatherContext weather,
            double userLat,
            double userLng,
            Map<Long, AiLogService.RecommendationDiversityPenalty> diversityPenalties
    ) {
        // 1. 후보가 없으면 즉시 종료
        if (candidates == null || candidates.isEmpty()) {
            log.warn("[장소 점수] 후보 없음 userId={}, petId={}, batchExecutionId={}",
                    RecommendationLogContext.userId(),
                    RecommendationLogContext.petId(),
                    RecommendationLogContext.batchExecutionId());
            return List.of();
        }

        // 2. 최근 추천 이력 기반 다양성 패널티를 반영해 재점수화
        Map<Long, AiLogService.RecommendationDiversityPenalty> safeDiversityPenalties =
                diversityPenalties == null ? Map.of() : diversityPenalties;
        List<ScoredPlace> rankedPlaces = rankPlaces(
                candidates,
                user,
                pet,
                weather,
                userLat,
                userLng,
                safeDiversityPenalties
        );

        // 3. 최종 정렬 결과를 로그로 남김
        log.info("[장소 점수] 상위 결과 userId={}, petId={}, batchExecutionId={}, count={}, topPlaces={}",
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                rankedPlaces.size(),
                rankedPlaces.stream()
                        .limit(3)
                        .map(scoredPlace -> scoredPlace.getPlace().getTitle() + "|" + scoredPlace.getTotalScore())
                        .collect(Collectors.joining(", ", "[", "]")));
        return rankedPlaces;
    }

    public List<ScoredPlace> scorePlaces(
            List<Place> candidates,
            User user,
            Pet pet,
            WeatherContext weather,
            double userLat,
            double userLng
    ) {
        // 장소가 없으면 빈 리스트를 반환합니다.
        if (candidates == null || candidates.isEmpty()) {
            log.warn("[장소 점수] 후보 없음 userId={}, petId={}, batchExecutionId={}",
                    RecommendationLogContext.userId(),
                    RecommendationLogContext.petId(),
                    RecommendationLogContext.batchExecutionId());
            return List.of();
        }

        // 후보 장소별 설명 가능한 점수를 계산한 뒤 총점 내림차순으로 정렬
        List<ScoredPlace> rankedPlaces = rankPlaces(
                candidates,
                user,
                pet,
                weather,
                userLat,
                userLng,
                Map.of()
        );
        log.info("[장소 점수] 상위 결과 userId={}, petId={}, batchExecutionId={}, count={}, topPlaces={}",
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                rankedPlaces.size(),
                rankedPlaces.stream()
                        .limit(3)
                        .map(scoredPlace -> scoredPlace.getPlace().getTitle() + "|" + scoredPlace.getTotalScore())
                        .collect(Collectors.joining(", ", "[", "]")));
        return rankedPlaces;
    }

    private List<ScoredPlace> rankPlaces(
            List<Place> candidates,
            User user,
            Pet pet,
            WeatherContext weather,
            double userLat,
            double userLng,
            Map<Long, AiLogService.RecommendationDiversityPenalty> diversityPenalties
    ) {
        return candidates.stream()
                .filter(this::passesPlaceRuleGate)
                .map(place -> scoreSinglePlace(
                        place,
                        user,
                        pet,
                        weather,
                        userLat,
                        userLng,
                        resolveDiversityPenalty(place, diversityPenalties, candidates.size())
                ))
                .sorted(Comparator.comparingDouble(ScoredPlace::getTotalScore).reversed())
                .toList();
    }

    /**
     * 단일 장소에 대해 상세 점수와 추천 이유를 계산한다.
     * 반려동물 적합도 50점, 날씨 적합도 20점, 장소 환경 15점,
     * 이동 편의성 10점, 부가 요소 5점을 합산하고
     * 조합 리스크 감점을 반영해 최종 점수를 만든다.
     *
     * @param place 점수를 계산할 후보 장소
     * @param user 추천 대상 사용자 정보
     * @param pet 추천 기준이 되는 반려동물 정보
     * @param weather 현재 추천에 반영할 날씨 정보
     * @param userLat 거리 계산에 사용할 사용자 위도
     * @param userLng 거리 계산에 사용할 사용자 경도
     * @return 프롬프트 생성과 추천 근거 설명에 사용할 단일 장소 점수 결과
     */
    public ScoredPlace scoreSinglePlace(
            Place place,
            User user,
            Pet pet,
            WeatherContext weather,
            double userLat,
            double userLng,
            double diversityPenalty
    ) {
        // 1. 섹션별 점수 계산
        SectionResult petResult = scorePetSuitability(place, pet); // 장소에 반려동물이 적합한지 점수를 계산합니다.
        SectionResult weatherResult = scoreWeatherSuitability(place, weather); // 장소에 날씨가 적합한지 점수를 계산합니다.
        SectionResult environmentResult = scoreEnvironment(place, pet); // 환경 점수를 계산합니다.
        SectionResult mobilityResult = scoreMobility(place, user, userLat, userLng); // 이동성 점수를 계산합니다.
        SectionResult bonusResult = scoreBonus(place, pet); // 보너스 점수를 계산합니다.

        // 2. 조합상 불리한 조건에 대한 감점 반영
        double penaltyScore = applyPenaltyIfNeeded(place, pet, weather) + diversityPenalty;
        // 3. 섹션 점수 합산하고 총점을 정규화 진행
        double rawTotal = petResult.score()
                + weatherResult.score()
                + environmentResult.score()
                + mobilityResult.score()
                + bonusResult.score()
                - penaltyScore;
        double totalScore = normalizeScoreIfNeeded(rawTotal);

        // 4. 프론트/로그/설명용 상세 점수 구조 생성
        List<ScoreBreakdown> breakdowns = List.of(
                petResult.toBreakdown(),
                weatherResult.toBreakdown(),
                environmentResult.toBreakdown(),
                mobilityResult.toBreakdown(),
                bonusResult.toBreakdown()
        );
        List<ScoreDetail> scoreDetails = flattenDetails(breakdowns);
        List<String> appliedBoosts = extractAppliedBoosts(scoreDetails);
        List<String> appliedPenalties = buildAppliedPenalties(place, pet, weather, diversityPenalty);
        Map<String, Double> sectionScores = buildSectionScores(
                petResult.score(),
                weatherResult.score(),
                environmentResult.score(),
                mobilityResult.score(),
                bonusResult.score()
        );

        // 5. 사용자에게 보여줄 요약 및 추천 이유 문장 생성
        String summary = buildSummary(place, breakdowns, penaltyScore);
        String reason = buildReason(place, weather, breakdowns, penaltyScore);

        logDebugScoreBreakdown(place, totalScore, penaltyScore, breakdowns);

        // 6. 최종 점수 결과 객체 반환
        return ScoredPlace.builder()
                .place(place)
                .totalScore(totalScore)
                .personalFitScore(petResult.score())
                .weatherFitScore(weatherResult.score())
                .environmentFitScore(environmentResult.score())
                .mobilityFitScore(mobilityResult.score())
                .bonusScore(bonusResult.score())
                .penaltyScore(penaltyScore)
                .sectionScores(sectionScores)
                .breakdowns(breakdowns)
                .scoreDetails(scoreDetails)
                .appliedBoosts(appliedBoosts)
                .appliedPenalties(appliedPenalties)
                .summary(summary)
                .reason(reason)
                .build();
    }

//    public ScoredPlace scoreSinglePlace(
//            Place place,
//            User user,
//            Pet pet,
//            WeatherContext weather,
//            double userLat,
//            double userLng
//    ) {
//        return scoreSinglePlace(place, user, pet, weather, userLat, userLng, 0.0);
//    }

    private double resolveDiversityPenalty(
            Place place,
            Map<Long, AiLogService.RecommendationDiversityPenalty> diversityPenalties,
            int candidateCount
    ) {
        if (place == null || place.getId() == null || diversityPenalties.isEmpty()) {
            return 0.0;
        }

        AiLogService.RecommendationDiversityPenalty diversityPenalty = diversityPenalties.get(place.getId());
        if (diversityPenalty == null) {
            return 0.0;
        }

        double adjustedPenalty = diversityPenalty.penalty();
        String reason = diversityPenalty.reason();

        if (candidateCount <= 2) {
            adjustedPenalty = 0.0;
            reason = reason + " (후보 부족으로 패널티 무시)";
        } else if (candidateCount <= 3) {
            adjustedPenalty = round(diversityPenalty.penalty() / 2.0);
            reason = reason + " (후보 부족으로 패널티 완화)";
        }

        if (adjustedPenalty <= 0.0) {
            return 0.0;
        }

        return adjustedPenalty;
    }

    /**
     * 반려동물 적합도 점수를 계산한다.
     * 나이 12점, 활동량 12점, 크기 10점, 성향 10점, 품종 6점으로 구성되며
     * 총 최대 50점까지 반영된다.
     *
     * @param place 장소
     * @param pet 반려동물
     * @return 반려동물 적합도 점수 결과
     */
    public SectionResult scorePetSuitability(Place place, Pet pet) {
        List<ScoreDetail> details = new ArrayList<>();
        if (pet == null) {
            details.add(detail("반려동물 적합도", "기본점수", 25.0, PET_MAX,
                    "반려동물 정보가 없어 중립 점수로 계산했습니다."));
            return new SectionResult("반려동물 적합도", 25.0, PET_MAX, "반려동물 정보 부족으로 중립 점수를 적용했습니다.", details);
        }

        // 텍스트로 변환
        String searchable = searchablePlaceText(place);

        // 나이, 활동량, 크기, 성향, 품종 점수를 합산해 반려동물 적합도를 계산
        double ageScore = scorePetAge(place, pet, searchable, details);
        double activityScore = scorePetActivity(place, pet, searchable, details);
        double sizeScore = scorePetSize(place, pet, searchable, details);
        double personalityScore = scorePetPersonality(place, pet, searchable, details);
        double breedScore = scoreBreedFit(place, pet, searchable, details);

        double score = clamp(ageScore + activityScore + sizeScore + personalityScore + breedScore, 0.0, PET_MAX);
        String summary = buildSectionSummary("반려동물 적합도", score, PET_MAX,
                List.of("나이/활동량/크기/성향 기반으로 장소와의 궁합을 계산했습니다."));
        return new SectionResult("반려동물 적합도", score, PET_MAX, summary, details);
    }


    /**
     * 날씨 적합도 점수를 계산한다.
     * 산책 등급 8점, 강수 4.5점, 기온 4.5점, 바람 3점으로 구성되며
     * 실내/실외 장소 특성과 현재 날씨 조건의 궁합을 반영한다.
     *
     * @param place 장소
     * @param weather 날씨 정보
     * @return 날씨 적합도 점수 결과
     */
    public SectionResult scoreWeatherSuitability(Place place, WeatherContext weather) {
        List<ScoreDetail> details = new ArrayList<>();
        if (weather == null) {
            details.add(detail("날씨 적합도", "기본점수", 10.0, WEATHER_MAX,
                    "날씨 정보가 없어 중립 점수로 계산했습니다."));
            return new SectionResult("날씨 적합도", 10.0, WEATHER_MAX, "날씨 정보 부족으로 중립 점수를 적용했습니다.", details);
        }

        boolean indoor = isIndoorPlace(place);
        boolean outdoor = isOutdoorPlace(place);
        boolean mixed = !indoor && !outdoor;

        double walkLevelScore = scoreWalkLevel(weather, indoor, outdoor, mixed, details);
        double precipitationScore = scorePrecipitation(weather, indoor, outdoor, mixed, details);
        double temperatureScore = scoreTemperature(weather, indoor, outdoor, mixed, details);
        double windScore = scoreWind(weather, indoor, outdoor, mixed, details);

        double score = clamp(walkLevelScore + precipitationScore + temperatureScore + windScore, 0.0, WEATHER_MAX);
        String summary = buildSectionSummary("날씨 적합도", score, WEATHER_MAX,
                List.of("산책 등급과 강수, 기온, 바람 조건을 반영했습니다."));
        return new SectionResult("날씨 적합도", score, WEATHER_MAX, summary, details);
    }

    /**
     * 장소 환경 적합도 점수를 계산한다.
     * 반려동물 동반 친화성 6점, 안전/쾌적성 5점, 품질 지표 4점으로 구성되며
     * 총 최대 15점까지 반영된다.
     *
     * @param place 장소
     * @param pet 반려동물
     * @return 환경 적합도 점수 결과
     */
    public SectionResult scoreEnvironment(Place place, Pet pet) {
        List<ScoreDetail> details = new ArrayList<>();
        String searchable = searchablePlaceText(place);

        double petFriendlyScore = scorePetFriendlyMetadata(place, searchable, details);
        double safetyScore = scoreSafetyAndComfort(place, searchable, details);
        double qualityScore = scorePlaceQuality(place, details);

        double score = clamp(petFriendlyScore + safetyScore + qualityScore, 0.0, ENVIRONMENT_MAX);
        String summary = buildSectionSummary("장소 환경 적합도", score, ENVIRONMENT_MAX,
                List.of("반려동물 동반 정보와 편의성, 품질 정보를 반영했습니다."));
        return new SectionResult("장소 환경 적합도", score, ENVIRONMENT_MAX, summary, details);
    }

    /**
     * 거리 및 이동 편의성 점수를 계산한다.
     * 사용자와 장소 간 거리 기준으로 최대 10점까지 부여하며,
     * 가까운 장소일수록 더 높은 점수를 받는다.
     *
     * @param place 장소
     * @param user 사용자
     * @param userLat 사용자 위도
     * @param userLng 사용자 경도
     * @return 이동 편의성 점수 결과
     */
    public SectionResult scoreMobility(Place place, User user, double userLat, double userLng) {
        List<ScoreDetail> details = new ArrayList<>();
        if (place.getLatitude() == null || place.getLongitude() == null) {
            details.add(detail("거리/이동 편의성", "좌표정보", 5.0, MOBILITY_MAX,
                    "좌표 정보가 없어 중립 점수로 계산했습니다."));
            return new SectionResult("거리/이동 편의성", 5.0, MOBILITY_MAX, "좌표가 없어 이동 편의성은 중립으로 계산했습니다.", details);
        }

        double distanceKm = distanceCalculator.calculateDistanceKm(
                userLat,
                userLng,
                place.getLatitude(),
                place.getLongitude()
        );

        /** 1차 필터를 통과한 후보들 사이에서 상대적으로 더 가까운 장소에 가점을 부여합니다. */
        double distanceScore = smoothDistanceScore(distanceKm);
        details.add(detail("거리/이동 편의성", "거리", distanceScore, MOBILITY_MAX,
                "사용자 기준 약 %.1fkm 거리입니다.".formatted(distanceKm)));

        String summary = buildSectionSummary("거리/이동 편의성", distanceScore, MOBILITY_MAX,
                List.of("1차 필터 내 후보 중 더 가까운 장소에 가점을 주었습니다."));
        return new SectionResult("거리/이동 편의성", distanceScore, MOBILITY_MAX, summary, details);
    }

    /**
     * 부가 요소 점수를 계산한다.
     * 대표견 선호 장소 일치, 검증 여부, 설명 품질, 태그 품질 등을 반영하며
     * 총 최대 5점까지 가산한다.
     *
     * @param place 장소
     * @param pet 반려동물
     * @return 부가 요소 점수 결과
     */
    public SectionResult scoreBonus(Place place, Pet pet) {
        List<ScoreDetail> details = new ArrayList<>();
        String searchable = searchablePlaceText(place);
        double score = 0.0;

        double preferredPlaceScore = scorePreferredPlaceBonus(place, pet, searchable, details);
        if (preferredPlaceScore > 0.0) {
            // 선호 장소와 장소 성격이 비슷하면 비슷한 점수 구간에서 조금 더 앞서도록 가산점을 부여합니다.
            score += preferredPlaceScore;
        }
        if (Boolean.TRUE.equals(place.getIsVerified())) {
            score += 1.2;
            details.add(detail("부가 요소", "검증 여부", 1.2, BONUS_MAX,
                    "검증된 장소라서 신뢰도를 보강했습니다."));
        }
        if (hasRichDescription(place)) {
            score += 1.0;
            details.add(detail("부가 요소", "설명 품질", 1.0, BONUS_MAX,
                    "설명이 비교적 풍부해 추천 설명 생성에 유리합니다."));
        }
        if (hasRichTags(place)) {
            score += 1.0;
            details.add(detail("부가 요소", "태그 품질", 1.0, BONUS_MAX,
                    "태그가 충분해 장소 성격을 더 잘 설명할 수 있습니다."));
        }

        score = clamp(score, 0.0, BONUS_MAX);
        String summary = buildSectionSummary("부가 요소", score, BONUS_MAX,
                List.of("선호 장소, 검증 여부, 메타데이터 풍부도를 반영했습니다."));
        return new SectionResult("부가 요소", score, BONUS_MAX, summary, details);
    }

    private double scorePreferredPlaceBonus(Place place, Pet pet, String searchable, List<ScoreDetail> details) {
        if (pet == null) {
            return 0.0;
        }

        String preferredPlace = RecommendationTextUtils.normalizeTrimLower(pet.getPreferredPlace());
        if (preferredPlace.isBlank()) {
            return 0.0;
        }

        boolean directMatch = hasKeyword(searchable, preferredPlace);
        boolean semanticMatch = matchesPreferredPlace(place, searchable, preferredPlace);
        double score = 0.0;
        String reason = null;

        if (directMatch && semanticMatch) {
            score = 3.8;
            reason = "대표견 선호 장소와 직접 일치하고 장소 성격도 유사합니다.";
        } else if (directMatch) {
            score = 3.0;
            reason = "대표견 선호 장소와 직접 일치하는 키워드가 감지되었습니다.";
        } else if (semanticMatch) {
            score = 2.2;
            reason = "대표견 선호 장소와 장소 카테고리/태그/설명이 유사하게 매칭되었습니다.";
        }

        if (score > 0.0) {
            details.add(detail("부가 요소", "대표견 선호 장소", score, BONUS_MAX, reason));
        }
        return score;
    }

    /**
     * 조합 리스크에 따른 감점을 계산한다.
     * 비 오는 날 야외 장소, 노령견과 활동형 야외 장소 조합,
     * 예민한 반려견과 혼잡 장소 조합 등을 감점 요소로 반영한다.
     *
     * @param place 장소
     * @param pet 반려동물
     * @param weather 날씨 정보
     * @return 조합 리스크 감점 점수
     */
    public double applyPenaltyIfNeeded(Place place, Pet pet, WeatherContext weather) {
        double penalty = 0.0;
        String searchable = searchablePlaceText(place); //
        boolean outdoor = isOutdoorPlace(place);
        boolean crowded = hasAnyKeyword(searchable, CROWDED_KEYWORDS);
        boolean activeOutdoor = hasAnyKeyword(searchable, ACTIVE_OUTDOOR_KEYWORDS);

        if (pet != null && pet.getPetAge() != null && pet.getPetAge() >= 10 && outdoor && activeOutdoor) {
            penalty += 2.5;
        }
        if (weather != null && weather.isRaining() && outdoor) {
            penalty += 2.0;
        }
        if (weather != null && weather.isWindy() && outdoor) {
            penalty += 1.0;
        }
        if (pet != null && isSensitivePet(pet) && crowded) {
            penalty += 1.5;
        }
        if (pet != null && pet.getPetActivity() == Pet.PetActivity.LOW && activeOutdoor) {
            penalty += 1.0;
        }
        penalty += personalizationMismatchPenalty(place, pet, searchable, crowded, activeOutdoor, outdoor);

        return round(clamp(penalty, 0.0, 8.0));
    }

    private double personalizationMismatchPenalty(
            Place place,
            Pet pet,
            String searchable,
            boolean crowded,
            boolean activeOutdoor,
            boolean outdoor
    ) {
        if (pet == null) {
            return 0.0;
        }

        double penalty = 0.0;
        boolean quietIndoor = hasAnyKeyword(searchable, QUIET_INDOOR_KEYWORDS);
        boolean wideOutdoor = hasAnyKeyword(searchable, WIDE_SPACE_KEYWORDS);

        if (pet.getPetActivity() == Pet.PetActivity.LOW && activeOutdoor && !quietIndoor) {
            penalty += 1.5;
        }
        if (pet.getPetSize() == Pet.PetSize.LARGE && !outdoor && !wideOutdoor) {
            penalty += 1.2;
        }
        if (isSensitivePet(pet) && (crowded || outdoor) && !quietIndoor) {
            penalty += 1.5;
        }

        String preferredPlace = RecommendationTextUtils.normalizeTrimLower(pet.getPreferredPlace());
        if (!preferredPlace.isBlank()) {
            boolean directMatch = hasKeyword(searchable, preferredPlace);
            boolean semanticMatch = matchesPreferredPlace(place, searchable, preferredPlace);
            if (!directMatch && !semanticMatch) {
                penalty += 1.0;
            }
        }

        int personalizationSignals = 0;
        if (!normalize(place.getOverview()).isBlank()) {
            personalizationSignals++;
        }
        if (!normalize(place.getBlogPositiveTags()).isBlank()) {
            personalizationSignals++;
        }
        if (!normalize(place.getTags()).isBlank()) {
            personalizationSignals++;
        }
        if (personalizationSignals == 0) {
            penalty += 0.8;
        }

        return penalty;
    }

    /**
     * 원시 점수를 0점 이상 100점 이하 범위로 보정하고 소수점 한 자리로 반올림한다.
     *
     * @param rawScore 정규화할 점수
     * @return 정규화된 점수
     */
    public double normalizeScoreIfNeeded(double rawScore) {
        return round(clamp(rawScore, 0.0, TOTAL_MAX));
    }

    private double scorePetAge(Place place, Pet pet, String searchable, List<ScoreDetail> details) {
        if (pet.getPetAge() == null) {
            details.add(detail("반려동물 적합도", "나이", 6.0, 12.0, "나이 정보가 없어 중립 점수를 적용했습니다."));
            return 6.0;
        }

        int age = pet.getPetAge();
        // 연령대에 따라 선호할 가능성이 높은 장소 특성을 반영
        if (age <= 2) {
            double score = hasAnyKeyword(searchable, ACTIVE_OUTDOOR_KEYWORDS) ? 10.5 : 7.5;
            details.add(detail("반려동물 적합도", "나이", score, 12.0, "어린 반려견은 활동 가능한 공간에서 강점을 가집니다."));
            return score;
        }
        if (age >= 10) {
            double score = hasAnyKeyword(searchable, QUIET_INDOOR_KEYWORDS) ? 10.0 : 6.5;
            details.add(detail("반려동물 적합도", "나이", score, 12.0, "노령견은 휴식 가능하고 자극이 적은 장소를 선호합니다."));
            return score;
        }

        double score = 9.0;
        details.add(detail("반려동물 적합도", "나이", score, 12.0, "성견 기준으로 무난한 적합도를 부여했습니다."));
        return score;
    }

    private double scorePetActivity(Place place, Pet pet, String searchable, List<ScoreDetail> details) {
        if (pet.getPetActivity() == null) {
            details.add(detail("반려동물 적합도", "활동량", 7.0, 12.0, "활동량 정보가 없어 중립 점수를 적용했습니다."));
            return 7.0;
        }
        // 활동량에 맞는 공간 성격(활동형/휴식형)을 가점 요소로 반영
        double score = switch (pet.getPetActivity()) {
            case HIGH -> hasAnyKeyword(searchable, ACTIVE_OUTDOOR_KEYWORDS) ? 11.0 : 6.5;
            case NORMAL -> 8.5;
            case LOW -> hasAnyKeyword(searchable, QUIET_INDOOR_KEYWORDS) ? 10.0 : 6.0;
        };
        details.add(detail("반려동물 적합도", "활동량", score, 12.0, "활동량과 장소 성격의 궁합을 반영했습니다."));
        return score;
    }

    private double scorePetSize(Place place, Pet pet, String searchable, List<ScoreDetail> details) {
        if (pet.getPetSize() == null) {
            details.add(detail("반려동물 적합도", "크기", 5.0, 10.0, "크기 정보가 없어 중립 점수를 적용했습니다."));
            return 5.0;
        }

        double score = switch (pet.getPetSize()) {
            case LARGE -> hasAnyKeyword(searchable, WIDE_SPACE_KEYWORDS) ? 8.5 : 5.5;
            case MEDIUM -> 7.0;
            case SMALL -> hasAnyKeyword(searchable, concat(QUIET_INDOOR_KEYWORDS, "소형")) ? 8.0 : 6.5;
        };
        details.add(detail("반려동물 적합도", "크기", score, 10.0, "반려견 크기와 장소 규모/밀도를 반영했습니다."));
        return score;
    }

    private double scorePetPersonality(Place place, Pet pet, String searchable, List<ScoreDetail> details) {
        String personality = RecommendationTextUtils.normalizeTrimLower(pet.getPersonality());
        if (personality.isBlank()) {
            details.add(detail("반려동물 적합도", "성향", 8.0, 10.0, "성향 정보가 없어 중립 점수를 적용했습니다."));
            return 8.0;
        }

        double score = 8.0;
        if ((personality.contains("활발") || personality.contains("에너지")) && hasAnyKeyword(searchable, ACTIVE_OUTDOOR_KEYWORDS)) {
            score += 1.5;
        }
        if ((personality.contains("예민") || personality.contains("소심") || personality.contains("겁")) && hasAnyKeyword(searchable, concat(QUIET_INDOOR_KEYWORDS, "아늑"))) {
            score += 1.5;
        }
        if ((personality.contains("사교") || personality.contains("친화")) && hasAnyKeyword(searchable, "카페", "라운지", "동반", "공원")) {
            score += 1.0;
        }

        score = clamp(score, 0.0, 10.0);
        details.add(detail("반려동물 적합도", "성향", score, 10.0, "성향 키워드와 장소 분위기의 궁합을 반영했습니다."));
        return score;
    }

    private double scoreBreedFit(Place place, Pet pet, String searchable, List<ScoreDetail> details) {
        String breed = RecommendationTextUtils.normalizeTrimLower(pet.getPetBreed());
        if (breed.isBlank()) {
            details.add(detail("반려동물 적합도", "품종", 4.5, 6.0, "품종 정보가 없어 중립 점수를 적용했습니다."));
            return 4.5;
        }

        double score = 4.5;
        if ((breed.contains("리트리버") || breed.contains("보더") || breed.contains("허스키")) && hasAnyKeyword(searchable, ACTIVE_OUTDOOR_KEYWORDS)) {
            score += 1.0;
        }
        if ((breed.contains("말티즈") || breed.contains("푸들") || breed.contains("포메")) && hasAnyKeyword(searchable, QUIET_INDOOR_KEYWORDS)) {
            score += 0.8;
        }

        score = clamp(score, 0.0, 6.0);
        details.add(detail("반려동물 적합도", "품종", score, 6.0, "품종 특성과 장소 유형의 일반적 궁합을 약하게 반영했습니다."));
        return score;
    }

    private double scoreWalkLevel(WeatherContext weather, boolean indoor, boolean outdoor, boolean mixed, List<ScoreDetail> details) {
        // 산책 가능 등급과 장소의 실내/실외 성격의 궁합을 점수화
        String walkLevel = RecommendationTextUtils.normalizeTrimLower(weather.getWalkLevel());
        double score = switch (walkLevel) {
            case "dangerous" -> indoor ? 8.0 : (mixed ? 5.0 : 2.5);
            case "caution" -> indoor ? 7.5 : (mixed ? 6.0 : 4.5);
            case "good" -> outdoor ? 8.0 : (mixed ? 7.0 : 6.0);
            default -> 6.0;
        };

        details.add(detail("날씨 적합도", "산책등급", score, 8.0, "walkLevel과 장소 실내/실외 성격을 반영했습니다."));
        return score;
    }

    private double scorePrecipitation(WeatherContext weather, boolean indoor, boolean outdoor, boolean mixed, List<ScoreDetail> details) {
        double score = 4.0;
        if (weather.isRaining()) {
            score = indoor ? 4.5 : (mixed ? 3.0 : 1.5);
        }
        details.add(detail("날씨 적합도", "강수", score, 4.5, "비/눈 등 강수 상황과 장소 성격을 반영했습니다."));
        return score;
    }

    private double scoreTemperature(WeatherContext weather, boolean indoor, boolean outdoor, boolean mixed, List<ScoreDetail> details) {
        double score = 4.0;
        if (weather.isHot()) {
            score = indoor ? 4.5 : (mixed ? 3.5 : 2.0);
        } else if (weather.isCold()) {
            score = indoor ? 4.5 : (mixed ? 3.2 : 2.0);
        }
        details.add(detail("날씨 적합도", "기온", score, 4.5, "폭염/한파 여부와 장소 보호성을 반영했습니다."));
        return score;
    }

    private double scoreWind(WeatherContext weather, boolean indoor, boolean outdoor, boolean mixed, List<ScoreDetail> details) {
        double score = weather.isWindy() ? (indoor ? 3.0 : (mixed ? 2.0 : 1.0)) : 3.0;
        details.add(detail("날씨 적합도", "바람", score, 3.0, "강풍 여부에 따른 체감 환경을 반영했습니다."));
        return score;
    }

    private double scorePetFriendlyMetadata(Place place, String searchable, List<ScoreDetail> details) {
        double score = 0.0;
        if (hasAnyKeyword(searchable, PET_FRIENDLY_KEYWORDS)) {
            score += 4.5;
        }
        if (hasAnyKeyword(searchable, CONVENIENCE_KEYWORDS)) {
            score += 1.5;
        }

        score = clamp(score, 0.0, 6.0);
        details.add(detail("장소 환경 적합도", "동반 친화성", score, 6.0, "반려동물 동반 관련 태그와 설명을 반영했습니다."));
        return score;
    }

    private double scoreSafetyAndComfort(Place place, String searchable, List<ScoreDetail> details) {
        double score = 4.5;
        if (hasAnyKeyword(searchable, SAFETY_COMFORT_KEYWORDS)) {
            score += 1.5;
        }
        if (hasAnyKeyword(searchable, concat(CROWDED_KEYWORDS, "행사장"))) {
            score -= 1.0;
        }

        score = clamp(score, 0.0, 5.0);
        details.add(detail("장소 환경 적합도", "안전/쾌적성", score, 5.0, "안전성과 혼잡 가능성을 함께 반영했습니다."));
        return score;
    }

    private double scorePlaceQuality(Place place, List<ScoreDetail> details) {
        double rating = place.getRating() == null ? 0.0 : place.getRating();
        int reviewCount = place.getReviewCount() == null ? 0 : place.getReviewCount();
        double aiRating = place.getAiRating() == null ? 0.0 : place.getAiRating();
        int blogCount = place.getBlogCount() == null ? 0 : place.getBlogCount();

        double ratingSignal = 0.0;
        if (reviewCount > 0 && rating > 0.0) {
            ratingSignal = Math.min(2.5, rating * 0.5);
        } else if (aiRating > 0.0) {
            ratingSignal = clamp(aiRating * 0.45, 0.8, 2.2);
        } else if (blogCount >= 200) {
            ratingSignal = 1.0;
        }

        double reliabilitySignal = 0.0;
        if (reviewCount >= 100) {
            reliabilitySignal = 1.5;
        } else if (reviewCount >= 30) {
            reliabilitySignal = 1.0;
        } else if (reviewCount > 0) {
            reliabilitySignal = 0.5;
        } else if (blogCount >= 1000) {
            reliabilitySignal = 1.4;
        } else if (blogCount >= 200) {
            reliabilitySignal = 1.0;
        } else if (blogCount >= 30) {
            reliabilitySignal = 0.6;
        }

        double score = clamp(ratingSignal + reliabilitySignal, 0.0, 4.0);

        score = clamp(score, 0.0, 4.0);
        details.add(detail("장소 환경 적합도", "품질 지표", score, 4.0, "공개 평점이 없으면 AI 평점과 블로그 표본 수로 보정했습니다."));
        return score;
    }

    /**
     * 엔티티 필드 기반으로 실제 추천 점수를 계산한다.
     * 펫 출입/정책 적합도 30점, 펫 편의시설 적합도 15점,
     * 장소 품질/신뢰도 20점, 블로그 감성 시그널 15점,
     * 태그/카테고리 적합도 20점을 합산하고 감점을 차감한다.
     */
    private ScoredPlace scorePlaceEntityDriven(Place place, double diversityPenalty) {
        SectionResult policyResult = scorePolicySuitability(place);
        SectionResult facilityResult = scorePetFacility(place);
        SectionResult qualityResult = scorePlaceTrustQuality(place);
        SectionResult sentimentResult = scoreBlogSignals(place);
        SectionResult contextResult = scoreContextFit(place);

        double penaltyScore = applyPlaceDataPenalty(place) + diversityPenalty;
        double totalScore = normalizeScoreIfNeeded(
                policyResult.score()
                        + facilityResult.score()
                        + qualityResult.score()
                        + sentimentResult.score()
                        + contextResult.score()
                        - penaltyScore
        );

        List<ScoreBreakdown> breakdowns = List.of(
                policyResult.toBreakdown(),
                facilityResult.toBreakdown(),
                qualityResult.toBreakdown(),
                sentimentResult.toBreakdown(),
                contextResult.toBreakdown()
        );

        return ScoredPlace.builder()
                .place(place)
                .totalScore(totalScore)
                .personalFitScore(policyResult.score())
                .weatherFitScore(facilityResult.score())
                .environmentFitScore(qualityResult.score())
                .mobilityFitScore(sentimentResult.score())
                .bonusScore(contextResult.score())
                .penaltyScore(penaltyScore)
                .sectionScores(buildSectionScores(breakdowns))
                .breakdowns(breakdowns)
                .scoreDetails(flattenDetails(breakdowns))
                .appliedBoosts(List.of())
                .appliedPenalties(List.of())
                .summary(buildSummary(place, breakdowns, penaltyScore))
                .reason(buildReason(place, null, breakdowns, penaltyScore))
                .build();
    }

    /**
     * 장소가 추천 대상 조건을 만족하는지 검증한다.
     * 반려동물 출입 불가, 운영 종료, 숙소인데 반려동물 수용 불가 같은 경우 제외한다.
     */
    private boolean passesPlaceRuleGate(Place place) {
        String combined = searchablePlaceEntityText(place);
        if (containsAny(combined,
                "반려동물 출입 불가", "반려동물 불가", "애견동반 불가", "동반 불가", "출입 금지", "전면 금지",
                "폐업", "운영 종료", "휴업")) {
            return false;
        }
        if ("STAY".equalsIgnoreCase(place.getCategory())) {
            String accom = normalize(place.getAccomCountPet());
            if (containsAny(accom, "0", "불가", "없음", "미제공")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 펫 출입 및 정책 적합도를 계산한다.
     * 실내 동반 가능성 12점, 정책 명확성 8점,
     * 제약 수준 6점, 카테고리 적합성 4점으로 구성되며
     * 총 최대 30점까지 반영된다.
     */
    private SectionResult scorePolicySuitability(Place place) {
        List<ScoreDetail> details = new ArrayList<>();
        String inside = normalize(place.getChkPetInside());
        String policy = normalize(place.getPetPolicy());
        String tags = normalize(place.getTags());
        String category = normalize(place.getCategory());

        double insideScore;
        String insideReason;
        if (isAffirmativeInside(inside) || containsAny(policy + " " + tags, "실내동반가능", "실내 가능", "동반 가능")) {
            insideScore = 12.0;
            insideReason = "실내 동반 가능 정보가 명시되어 있습니다.";
        } else if (containsAny(policy + " " + tags, "실내 불가", "실내불가", "야외만 가능", "실외만 가능")) {
            insideScore = 4.0;
            insideReason = "실내는 제한되지만 야외 동반은 가능한 것으로 해석했습니다.";
        } else {
            insideScore = 6.0;
            insideReason = "실내 동반 정보가 명확하지 않아 중립 점수를 부여했습니다.";
        }
        details.add(detail("펫 출입/정책 적합도", "실내 동반 가능성", insideScore, POLICY_MAX, insideReason));

        double clarityScore = policy.isBlank() || isUnknown(policy) ? 2.0 : (policy.length() >= 12 ? 8.0 : 5.0);
        details.add(detail("펫 출입/정책 적합도", "정책 명확성", clarityScore, POLICY_MAX,
                clarityScore >= 8.0 ? "펫 정책 문구가 충분히 구체적입니다." : "정책 정보가 부족하거나 짧습니다."));

        double restrictionScore = 6.0;
        String restrictionReason = "과도한 제약이 확인되지 않았습니다.";
        if (containsAny(policy, "대형견 불가", "중형견 불가", "소형견만", "특정 구역만", "사전 문의")) {
            restrictionScore = 2.0;
            restrictionReason = "크기 또는 구역 제한이 있어 제약 점수를 낮췄습니다.";
        } else if (containsAny(policy, "목줄", "이동가방", "배변 처리", "기본 에티켓")) {
            restrictionScore = 4.0;
            restrictionReason = "기본적인 에티켓 규칙만 확인됩니다.";
        }
        details.add(detail("펫 출입/정책 적합도", "제약 수준", restrictionScore, POLICY_MAX, restrictionReason));

        double categoryFitScore = 2.0;
        if ("stay".equals(category) && !normalize(place.getAccomCountPet()).isBlank()) {
            categoryFitScore = 4.0;
        } else if (containsAny(policy + " " + tags, "펫프렌들리", "반려동물 동반", "애견동반")) {
            categoryFitScore = 4.0;
        } else if (containsAny(policy, "제한", "불가")) {
            categoryFitScore = 0.0;
        }
        details.add(detail("펫 출입/정책 적합도", "카테고리 적합성", categoryFitScore, POLICY_MAX,
                categoryFitScore >= 4.0 ? "카테고리 특성에 맞는 동반 정책이 있습니다." : "카테고리 대비 정책 신호가 약합니다."));

        double score = round(clamp(insideScore + clarityScore + restrictionScore + categoryFitScore, 0.0, POLICY_MAX));
        return new SectionResult(
                "펫 출입/정책 적합도",
                score,
                POLICY_MAX,
                buildSectionSummary("펫 출입/정책 적합도", score, POLICY_MAX,
                        List.of("실내 가능 여부, 정책 명확성, 제약 수준을 기준으로 계산했습니다.")),
                details
        );
    }

    /**
     * 펫 편의시설 적합도를 계산한다.
     * 기본 시설 9점, 이용 편의 4점, 안전/쾌적성 2점으로 구성되며
     * 총 최대 15점까지 반영된다.
     */
    private SectionResult scorePetFacility(Place place) {
        List<ScoreDetail> details = new ArrayList<>();
        String facility = normalize(place.getPetFacility());
        String positive = normalize(place.getBlogPositiveTags());
        String tags = normalize(place.getTags());

        int facilityMatches = countKeywords(facility,
                "전용 공간", "전용공간", "배변", "물그릇", "식기", "놀이터", "운동장", "산책로", "어메니티", "전용 객실", "전용객실");
        double baseFacility = facilityMatches >= 3 ? 9.0 : facilityMatches == 2 ? 6.0 : facilityMatches == 1 ? 3.0 : 1.0;
        details.add(detail("펫 편의시설 적합도", "기본 시설", baseFacility, FACILITY_MAX,
                facilityMatches > 0 ? "펫 시설 정보가 확인됩니다." : "시설 정보가 부족해 기본 점수만 반영했습니다."));

        int convenienceMatches = countKeywords(tags + " " + positive,
                "주차", "넓음", "산책로", "잔디", "전용 객실", "쾌적", "동선 편함");
        double convenienceBonus = Math.min(4.0, convenienceMatches * 1.3);
        details.add(detail("펫 편의시설 적합도", "이용 편의", convenienceBonus, FACILITY_MAX,
                convenienceMatches > 0 ? "이용 편의를 뒷받침하는 태그/후기가 있습니다." : "편의 관련 신호가 약합니다."));

        int safetyMatches = countKeywords(positive + " " + facility,
                "안전", "조용", "청결", "쾌적", "관리");
        double safetyBonus = Math.min(2.0, safetyMatches * 0.7);
        details.add(detail("펫 편의시설 적합도", "안전/쾌적성", safetyBonus, FACILITY_MAX,
                safetyMatches > 0 ? "안전성과 쾌적성 후기가 확인됩니다." : "안전성 관련 추가 신호가 없습니다."));

        double score = round(clamp(baseFacility + convenienceBonus + safetyBonus, 0.0, FACILITY_MAX));
        return new SectionResult(
                "펫 편의시설 적합도",
                score,
                FACILITY_MAX,
                buildSectionSummary("펫 편의시설 적합도", score, FACILITY_MAX,
                        List.of("펫 시설, 이용 편의 태그, 청결/안전 신호를 반영했습니다.")),
                details
        );
    }

    /**
     * 장소 품질 및 신뢰도 점수를 계산한다.
     * 공개 평점 8점, 리뷰 표본 신뢰도 5점,
     * AI 보조 평점 3점, 블로그 표본 신뢰도 4점으로 구성되며
     * 총 최대 20점까지 반영된다.
     */
    private SectionResult scorePlaceTrustQuality(Place place) {
        List<ScoreDetail> details = new ArrayList<>();
        double rating = place.getRating() == null ? 0.0 : place.getRating();
        int reviewCount = place.getReviewCount() == null ? 0 : place.getReviewCount();
        double aiRating = place.getAiRating() == null ? 0.0 : place.getAiRating();
        int blogCount = place.getBlogCount() == null ? 0 : place.getBlogCount();

        double publicRatingScore = clamp((rating / 5.0) * 8.0, 0.0, 8.0);
        double reviewReliabilityScore = reviewCount >= 300 ? 5.0
                : reviewCount >= 100 ? 4.0
                : reviewCount >= 30 ? 3.0
                : reviewCount >= 10 ? 2.0
                : reviewCount > 0 ? 1.0 : 0.0;
        double aiAssistScore = place.getAiRating() == null ? 1.0
                : aiRating >= 4.5 ? 3.0
                : aiRating >= 4.0 ? 2.5
                : aiRating >= 3.5 ? 2.0
                : aiRating >= 3.0 ? 1.0 : 0.5;
        double blogReliabilityScore = blogCount >= 200 ? 4.0
                : blogCount >= 80 ? 3.0
                : blogCount >= 20 ? 2.0
                : blogCount >= 5 ? 1.0 : 0.0;

        details.add(detail("장소 품질/신뢰도", "공개 평점", publicRatingScore, QUALITY_MAX,
                "평점 %.1f/5.0을 8점 만점으로 환산했습니다.".formatted(rating)));
        details.add(detail("장소 품질/신뢰도", "리뷰 표본 신뢰도", reviewReliabilityScore, QUALITY_MAX,
                "리뷰 수 %d개를 기준으로 신뢰도를 계산했습니다.".formatted(reviewCount)));
        details.add(detail("장소 품질/신뢰도", "AI 보조 평점", aiAssistScore, QUALITY_MAX,
                place.getAiRating() == null ? "AI 평점 정보가 없어 중립 보정했습니다." : "AI 평점은 보조 신호로만 사용했습니다."));
        details.add(detail("장소 품질/신뢰도", "블로그 표본 신뢰도", blogReliabilityScore, QUALITY_MAX,
                "블로그 언급 수 %d건을 반영했습니다.".formatted(blogCount)));

        double score = publicRatingScore + reviewReliabilityScore + aiAssistScore + blogReliabilityScore;
        if (reviewCount < 10 && blogCount < 5) {
            score *= 0.8;
        }
        score = round(clamp(score, 0.0, QUALITY_MAX));
        return new SectionResult(
                "장소 품질/신뢰도",
                score,
                QUALITY_MAX,
                buildSectionSummary("장소 품질/신뢰도", score, QUALITY_MAX,
                        List.of("평점, 리뷰 수, AI 평점, 블로그 표본 수를 결합했습니다.")),
                details
        );
    }

    /**
     * 블로그 감성 시그널 점수를 계산한다.
     * 긍정 태그 가점과 부정 태그 감점을 함께 반영하며
     * 총 최대 15점 범위에서 실제 체감 품질을 점수화한다.
     */
    private SectionResult scoreBlogSignals(Place place) {
        List<ScoreDetail> details = new ArrayList<>();
        String positive = normalize(place.getBlogPositiveTags());
        String negative = normalize(place.getBlogNegativeTags());

        double positiveScore = 0.0;
        positiveScore += countKeywords(positive, "친절", "재방문", "펫친화") * 1.5;
        positiveScore += countKeywords(positive, "청결", "쾌적", "조용", "넓음", "산책", "사진") * 1.2;
        positiveScore = clamp(positiveScore, 0.0, 10.0);

        double negativePenalty = 0.0;
        negativePenalty += countKeywords(negative, "혼잡", "붐빔", "소음") * 1.5;
        negativePenalty += countKeywords(negative, "냄새", "청결불량", "불친절") * 2.0;
        negativePenalty += countKeywords(negative, "실내불가", "펫제한", "대기") * 1.5;
        negativePenalty = clamp(negativePenalty, 0.0, 8.0);

        details.add(detail("블로그 감성 시그널", "긍정 태그", positiveScore, SENTIMENT_MAX,
                positive.isBlank() ? "긍정 블로그 태그가 없어 중립 처리했습니다." : "긍정 블로그 태그를 가중 합산했습니다."));
        details.add(detail("블로그 감성 시그널", "부정 태그 감점", SENTIMENT_MAX - negativePenalty, SENTIMENT_MAX,
                negative.isBlank() ? "부정 블로그 태그가 없습니다." : "혼잡/제약/청결 관련 부정 태그를 감점했습니다."));

        double score = round(clamp(5.0 + positiveScore - negativePenalty, 0.0, SENTIMENT_MAX));
        return new SectionResult(
                "블로그 감성 시그널",
                score,
                SENTIMENT_MAX,
                buildSectionSummary("블로그 감성 시그널", score, SENTIMENT_MAX,
                        List.of("블로그 긍정/부정 태그로 실제 체감 품질을 반영했습니다.")),
                details
        );
    }

    /**
     * 태그 및 카테고리 적합도를 계산한다.
     * 카테고리 기본점 4점, 태그 적합도 6점,
     * 숙소 특화 보정 5점, 데이터 완성도 5점으로 구성되며
     * 총 최대 20점까지 반영된다.
     */
    private SectionResult scoreContextFit(Place place) {
        List<ScoreDetail> details = new ArrayList<>();
        String tags = normalize(place.getTags());
        String category = normalize(place.getCategory());
        String positive = normalize(place.getBlogPositiveTags());
        String policy = normalize(place.getPetPolicy());
        String facility = normalize(place.getPetFacility());

        double categoryBase = "stay".equals(category) || "dining".equals(category) || "place".equals(category) ? 4.0 : 2.0;
        int tagMatches = countKeywords(tags + " " + positive,
                "실내동반가능", "조용", "산책로", "주차", "잔디", "펫프렌들리", "넓음", "전용객실", "애견동반");
        double tagCoverage = tagMatches >= 3 ? 6.0 : tagMatches == 2 ? 4.0 : tagMatches == 1 ? 2.0 : 0.0;

        double stayBonus = 0.0;
        if ("stay".equals(category)) {
            String accom = normalize(place.getAccomCountPet());
            if (!accom.isBlank() && !isUnknown(accom)) {
                stayBonus += 2.0;
            }
            if (containsAny(policy, "전용 객실", "객실 내 동반", "추가 요금", "사전 문의")) {
                stayBonus += 2.0;
            }
            if (containsAny(facility, "어메니티", "배변", "식기", "쿠션", "방석")) {
                stayBonus += 1.0;
            }
        }

        int filledFields = countFilled(
                place.getTags(),
                place.getAiRating(),
                place.getBlogCount(),
                place.getBlogPositiveTags(),
                place.getBlogNegativeTags(),
                place.getPetPolicy(),
                place.getPetFacility(),
                place.getRating() == null && place.getReviewCount() == null ? null : "rating"
        );
        double completenessScore = clamp((filledFields / 8.0) * 5.0, 0.0, 5.0);

        details.add(detail("태그/카테고리 적합도", "카테고리 기본점", categoryBase, CONTEXT_MAX,
                "추천 대상 카테고리 여부를 반영했습니다."));
        details.add(detail("태그/카테고리 적합도", "태그 적합도", tagCoverage, CONTEXT_MAX,
                tagMatches > 0 ? "추천 설명에 활용 가능한 핵심 태그가 존재합니다." : "핵심 태그가 부족합니다."));
        details.add(detail("태그/카테고리 적합도", "숙소 특화 보정", stayBonus, CONTEXT_MAX,
                stayBonus > 0.0 ? "숙소형 장소의 펫 동반 운영 정보를 추가 반영했습니다." : "숙소 특화 보정은 적용되지 않았습니다."));
        details.add(detail("태그/카테고리 적합도", "데이터 완성도", completenessScore, CONTEXT_MAX,
                "설명 가능한 추천을 위한 핵심 필드 채움률을 반영했습니다."));

        double score = round(clamp(categoryBase + tagCoverage + stayBonus + completenessScore, 0.0, CONTEXT_MAX));
        return new SectionResult(
                "태그/카테고리 적합도",
                score,
                CONTEXT_MAX,
                buildSectionSummary("태그/카테고리 적합도", score, CONTEXT_MAX,
                        List.of("카테고리, 핵심 태그, 숙소 운영 정보, 데이터 완성도를 반영했습니다.")),
                details
        );
    }

    /**
     * 장소 데이터 자체의 제약 조건에 따른 감점을 계산한다.
     * 실내 불가, 크기 제한, 추가 요금, 혼잡/소음/청결 불량 등의 부정 신호를 반영한다.
     */
    private double applyPlaceDataPenalty(Place place) {
        String policy = normalize(place.getPetPolicy());
        String negative = normalize(place.getBlogNegativeTags());
        String tags = normalize(place.getTags());
        double penalty = 0.0;

        if (containsAny(policy + " " + tags, "실내 불가", "실내불가", "야외만 가능", "실외만 가능")) {
            penalty += 8.0;
        }
        if (containsAny(policy, "소형견만", "대형견 불가", "사전 문의", "추가 요금")) {
            penalty += 4.0;
        }
        penalty += Math.min(8.0, countKeywords(negative, "혼잡", "붐빔", "소음", "냄새", "청결불량", "대기", "불친절") * 1.5);

        return round(clamp(penalty, 0.0, 15.0));
    }

    private Map<String, Double> buildSectionScores(List<ScoreBreakdown> breakdowns) {
        Map<String, Double> sectionScores = new LinkedHashMap<>();
        for (ScoreBreakdown breakdown : breakdowns) {
            sectionScores.put(breakdown.getSection(), round(breakdown.getScore()));
        }
        return sectionScores;
    }

    private String searchablePlaceEntityText(Place place) {
        return List.of(
                        normalize(place.getCategory()),
                        normalize(place.getTitle()),
                        normalize(place.getDescription()),
                        normalize(place.getOverview()),
                        normalize(place.getTags()),
                        normalize(place.getPetPolicy()),
                        normalize(place.getPetFacility()),
                        normalize(place.getBlogPositiveTags()),
                        normalize(place.getBlogNegativeTags()),
                        normalize(place.getChkPetInside()),
                        normalize(place.getAccomCountPet())
                ).stream()
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        return RecommendationTextUtils.normalizeTrimLower(value);
    }

    private boolean isUnknown(String value) {
        return containsAny(normalize(value), "정보 없음", "없음", "미상", "unknown", "n/a");
    }

    private boolean isAffirmativeInside(String inside) {
        return "y".equalsIgnoreCase(inside) || containsAny(inside, "가능", "허용", "yes");
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = normalize(text);
        for (String keyword : keywords) {
            if (normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private int countKeywords(String text, String... keywords) {
        String normalized = normalize(text);
        int count = 0;
        for (String keyword : keywords) {
            if (normalized.contains(normalize(keyword))) {
                count++;
            }
        }
        return count;
    }

    private int countFilled(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String stringValue) {
                if (!normalize(stringValue).isBlank() && !isUnknown(stringValue)) {
                    count++;
                }
                continue;
            }
            count++;
        }
        return count;
    }

    private Map<String, Double> buildSectionScores(
            double petScore,
            double weatherScore,
            double environmentScore,
            double mobilityScore,
            double bonusScore
    ) {
        Map<String, Double> sectionScores = new LinkedHashMap<>();
        sectionScores.put("반려동물 적합도", round(petScore));
        sectionScores.put("날씨 적합도", round(weatherScore));
        sectionScores.put("장소 환경 적합도", round(environmentScore));
        sectionScores.put("거리/이동 편의성", round(mobilityScore));
        sectionScores.put("부가 요소", round(bonusScore));
        return sectionScores;
    }

    /**
     * 점수 Breakdown 목록을 ScoreDetail 목록으로 평탄화한다.
     * 로그, 응답, 설명 생성용 세부 점수 구조를 만들 때 사용한다.
     */
    private List<ScoreDetail> flattenDetails(List<ScoreBreakdown> breakdowns) {
        return breakdowns.stream()
                .flatMap(breakdown -> breakdown.getDetails().stream())
                .toList();
    }

    private List<String> extractAppliedBoosts(List<ScoreDetail> scoreDetails) {
        if (scoreDetails == null || scoreDetails.isEmpty()) {
            return List.of();
        }

        return scoreDetails.stream()
                .filter(detail -> detail.getScore() > 0.0)
                .filter(detail -> detail.getScore() >= Math.max(1.0, detail.getMaxScore() * 0.45))
                .sorted(Comparator
                        .comparingDouble((ScoreDetail detail) -> detail.getScore() / Math.max(detail.getMaxScore(), 1.0))
                        .reversed()
                        .thenComparing(ScoreDetail::getScore, Comparator.reverseOrder()))
                .limit(3)
                .map(detail -> "%s +%.1f".formatted(detail.getItem(), round(detail.getScore())))
                .toList();
    }

    private List<String> buildAppliedPenalties(Place place, Pet pet, WeatherContext weather, double diversityPenalty) {
        List<String> penalties = new ArrayList<>();
        String searchable = searchablePlaceText(place);
        boolean outdoor = isOutdoorPlace(place);
        boolean crowded = hasAnyKeyword(searchable, CROWDED_KEYWORDS);
        boolean activeOutdoor = hasAnyKeyword(searchable, ACTIVE_OUTDOOR_KEYWORDS);
        boolean quietIndoor = hasAnyKeyword(searchable, QUIET_INDOOR_KEYWORDS);
        boolean wideOutdoor = hasAnyKeyword(searchable, WIDE_SPACE_KEYWORDS);

        if (pet != null && pet.getPetAge() != null && pet.getPetAge() >= 10 && outdoor && activeOutdoor) {
            penalties.add("노령 반려동물 야외활동 -2.5");
        }
        if (weather != null && weather.isRaining() && outdoor) {
            penalties.add("비 오는 날 야외 -2.0");
        }
        if (weather != null && weather.isWindy() && outdoor) {
            penalties.add("강풍 야외 -1.0");
        }
        if (pet != null && isSensitivePet(pet) && crowded) {
            penalties.add("예민 성향과 혼잡 장소 -1.5");
        }
        if (pet != null && pet.getPetActivity() == Pet.PetActivity.LOW && activeOutdoor) {
            penalties.add("저활동 성향과 활동형 장소 -1.0");
        }
        if (pet != null && pet.getPetActivity() == Pet.PetActivity.LOW && activeOutdoor && !quietIndoor) {
            penalties.add("저활동 맞춤도 부족 -1.5");
        }
        if (pet != null && pet.getPetSize() == Pet.PetSize.LARGE && !outdoor && !wideOutdoor) {
            penalties.add("대형견 대비 공간 협소 추정 -1.2");
        }
        if (pet != null && isSensitivePet(pet) && (crowded || outdoor) && !quietIndoor) {
            penalties.add("예민 성향 대비 안정성 부족 -1.5");
        }
        if (pet != null) {
            String preferredPlace = RecommendationTextUtils.normalizeTrimLower(pet.getPreferredPlace());
            if (!preferredPlace.isBlank()) {
                boolean directMatch = hasKeyword(searchable, preferredPlace);
                boolean semanticMatch = matchesPreferredPlace(place, searchable, preferredPlace);
                if (!directMatch && !semanticMatch) {
                    penalties.add("선호 장소와 비매칭 -1.0");
                }
            }
        }

        int personalizationSignals = 0;
        if (!normalize(place.getOverview()).isBlank()) {
            personalizationSignals++;
        }
        if (!normalize(place.getBlogPositiveTags()).isBlank()) {
            personalizationSignals++;
        }
        if (!normalize(place.getTags()).isBlank()) {
            personalizationSignals++;
        }
        if (personalizationSignals == 0) {
            penalties.add("개인화 신호 부족 -0.8");
        }
        if (diversityPenalty > 0.0) {
            penalties.add("최근 추천 중복 방지 -%.1f".formatted(round(diversityPenalty)));
        }

        return penalties.stream().limit(4).toList();
    }

    /**
     * 점수가 높은 상위 섹션들을 중심으로 사용자에게 보여줄 요약 문장을 생성합니다.
     *
     * 상위 2개 섹션의 요약을 합쳐서, 가장 중요한 요소를 강조하고, 감점이 있을 경우 감점 정보를 추가합니다.
     */
    private void logDebugScoreBreakdown(
            Place place,
            double totalScore,
            double penaltyScore,
            List<ScoreBreakdown> breakdowns
    ) {
        if (!log.isDebugEnabled()) {
            return;
        }

        String sectionSummary = breakdowns.stream()
                .map(this::formatSectionBreakdown)
                .collect(Collectors.joining(System.lineSeparator()));

        log.debug(
                "[장소 점수 상세] 장소={}, 총점={}, 감점={}{}{}",
                place.getTitle(),
                round(totalScore),
                round(penaltyScore),
                System.lineSeparator(),
                sectionSummary
        );
    }

    private String formatSectionBreakdown(ScoreBreakdown breakdown) {
        String details = breakdown.getDetails().stream()
                .map(this::formatScoreDetail)
                .collect(Collectors.joining(System.lineSeparator()));

        if (details.isBlank()) {
            return "  - %s: %.1f/%.1f".formatted(
                    breakdown.getSection(),
                    round(breakdown.getScore()),
                    breakdown.getMaxScore()
            );
        }

        return "  - %s: %.1f/%.1f%s%s".formatted(
                breakdown.getSection(),
                round(breakdown.getScore()),
                breakdown.getMaxScore(),
                System.lineSeparator(),
                details
        );
    }

    private String formatScoreDetail(ScoreDetail detail) {
        return "    * %s: %.1f/%.1f - %s".formatted(
                detail.getItem(),
                round(detail.getScore()),
                detail.getMaxScore(),
                detail.getReason()
        );
    }

    /**
     * 상위 점수 섹션을 기반으로 추천 요약 문장을 생성한다.
     * 강점이 높은 항목 위주로 요약하고 감점이 있으면 함께 표시한다.
     */
    private String buildSummary(Place place, List<ScoreBreakdown> breakdowns, double penaltyScore) {
        String topSummary = breakdowns.stream()
                .sorted(Comparator.comparingDouble(ScoreBreakdown::getScore).reversed())
                .limit(2)
                .map(ScoreBreakdown::getSummary)
                .reduce((a, b) -> a + " / " + b)
                .orElse("전반적으로 균형 잡힌 후보입니다.");

        if (penaltyScore > 0.0) {
            return "%s 감점 %.1f점이 반영되었습니다.".formatted(topSummary, penaltyScore);
        }
        return topSummary;
    }

    /**
     * 추천 강점과 현재 날씨 상황, 감점 여부를 포함한 설명 문장을 생성합니다.
     *
     * 상위 3개 섹션의 요약을 합쳐서, 가장 중요한 요소를 강조하고, 감점이 있을 경우 감점 정보를 추가합니다.
     */
    private String buildReason(Place place, WeatherContext weather, List<ScoreBreakdown> breakdowns, double penaltyScore) {
        StringBuilder reason = new StringBuilder();
        reason.append(place.getTitle()).append("은(는) ");

        List<String> parts = breakdowns.stream()
                .sorted(Comparator.comparingDouble(ScoreBreakdown::getScore).reversed())
                .limit(3)
                .map(ScoreBreakdown::getSection)
                .toList();

        reason.append(String.join(", ", parts)).append(" 항목에서 강점을 보였습니다.");
        if (weather != null && weather.getWalkLevel() != null) {
            reason.append(" 현재 산책 등급은 ").append(weather.getWalkLevel()).append("입니다.");
        }
        if (penaltyScore > 0.0) {
            reason.append(" 다만 상황상 ").append(round(penaltyScore)).append("점 감점이 반영되었습니다.");
        }
        return reason.toString();
    }

    private String buildSectionSummary(String section, double score, double maxScore, List<String> reasons) {
        return "%s %.1f/%.1f점. %s".formatted(section, round(score), maxScore, String.join(" ", reasons));
    }

    private boolean isIndoorPlace(Place place) {
        String searchable = searchablePlaceText(place);
        return hasAnyKeyword(searchable, concat(QUIET_INDOOR_KEYWORDS, "전시", "박물관"))
                || ("DINING".equalsIgnoreCase(place.getCategory()) && !hasAnyKeyword(searchable, "야외", "실외"));
    }

    private boolean isOutdoorPlace(Place place) {
        String searchable = searchablePlaceText(place);
        return hasAnyKeyword(searchable, OUTDOOR_PLACE_KEYWORDS);
    }

    private boolean matchesPreferredPlace(Place place, String searchable, String preferredPlace) {
        if (preferredPlace.contains("실내카페")) {
            return "DINING".equalsIgnoreCase(place.getCategory())
                    && hasAnyKeyword(searchable, concat(QUIET_INDOOR_KEYWORDS, "실내가능", "동반"));
        }
        if (preferredPlace.contains("공원")) {
            return "PLACE".equalsIgnoreCase(place.getCategory())
                    && hasAnyKeyword(searchable, concat(OUTDOOR_PLACE_KEYWORDS, "넓음"));
        }
        if (preferredPlace.contains("넓은 야외")) {
            return "PLACE".equalsIgnoreCase(place.getCategory())
                    && hasAnyKeyword(searchable, WIDE_SPACE_KEYWORDS);
        }
        return false;
    }

    private boolean hasRichDescription(Place place) {
        String description = normalize(place.getDescription());
        String overview = normalize(place.getOverview());
        return description.length() >= 40 || overview.length() >= 60;
    }

    private boolean hasRichTags(Place place) {
        String tags = normalize(place.getTags());
        String positiveTags = normalize(place.getBlogPositiveTags());
        int tagCount = tags.isBlank() ? 0 : tags.split(",").length;
        int positiveTagCount = positiveTags.isBlank() ? 0 : positiveTags.split(",").length;
        return tagCount >= 3 || positiveTagCount >= 2;
    }

    private boolean isSensitivePet(Pet pet) {
        String personality = RecommendationTextUtils.normalizeTrimLower(pet.getPersonality());
        return personality.contains("예민") || personality.contains("소심") || personality.contains("겁");
    }

    /**
     * 사용자와 장소 간 거리값을 부드럽게 점수화한다.
     * 거리가 가까울수록 높은 점수를 부여하며 최대 10점까지 반영한다.
     */
    private double smoothDistanceScore(double distanceKm) {
        double score = 10.0 / (1.0 + (distanceKm / 3.0));
        return round(clamp(score, 1.0, MOBILITY_MAX));
    }

    /**
     * 점수 세부 항목 객체를 생성한다.
     * 섹션명, 항목명, 점수, 최대 점수, 이유를 함께 저장한다.
     */
    private ScoreDetail detail(String section, String item, double score, double maxScore, String reason) {
        return ScoreDetail.builder()
                .section(section)
                .item(item)
                .score(round(score))
                .maxScore(maxScore)
                .reason(reason)
                .build();
    }

    /**
     * 카테고리, 제목, 설명, 태그를 하나의 검색용 텍스트로 합쳐
     * 키워드 기반 점수 계산에 사용합니다.
     */
    private String searchablePlaceText(Place place) {
        return List.of(
                        RecommendationTextUtils.normalizeTrimLower(place.getCategory()),
                        RecommendationTextUtils.normalizeTrimLower(place.getTitle()),
                        RecommendationTextUtils.normalizeTrimLower(place.getDescription()),
                        RecommendationTextUtils.normalizeTrimLower(place.getOverview()),
                        RecommendationTextUtils.normalizeTrimLower(place.getTags()),
                        RecommendationTextUtils.normalizeTrimLower(place.getBlogPositiveTags()),
                        RecommendationTextUtils.normalizeTrimLower(place.getBlogNegativeTags()),
                        RecommendationTextUtils.normalizeTrimLower(place.getPetPolicy()),
                        RecommendationTextUtils.normalizeTrimLower(place.getPetFacility()),
                        RecommendationTextUtils.normalizeTrimLower(place.getChkPetInside()),
                        RecommendationTextUtils.normalizeTrimLower(place.getAccomCountPet())
                ).stream()
                .filter(text -> !text.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    /**
     * 텍스트에 특정 키워드가 포함되어 있는지 확인합니다.
     * @param text 검사할 텍스트
     * @param keyword 검사할 키워드
     * @return 키워드가 포함되어 있으면 true, 아니면 false
     */
    private boolean hasKeyword(String text, String keyword) {
        String normalizedKeyword = RecommendationTextUtils.normalizeTrimLower(keyword);
        return !normalizedKeyword.isBlank() && text.contains(normalizedKeyword);
    }

    /**
     * 사용자 위도를 검증 후 반환한다.
     */
    private double requireUserLatitude(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return user.getLatitude();
    }

    /**
     * 사용자 경도를 검증 후 반환한다.
     */
    private double requireUserLongitude(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return user.getLongitude();
    }

    /**
     * 텍스트에 여러 키워드 중 하나라도 포함되어 있는지 확인합니다.
     * @param text 검사할 텍스트
     * @param keywords 검사할 키워드 목록
     * @return 키워드 중 하나라도 포함되어 있으면 true, 아니면 false
     */
    private boolean hasAnyKeyword(String text, String... keywords) {
        for (String keyword : keywords) {
            if (hasKeyword(text, keyword)) {
                return true;
            }
        }
        return false;
    }

    private String[] concat(String[] keywords, String... extraKeywords) {
        String[] result = new String[keywords.length + extraKeywords.length];
        System.arraycopy(keywords, 0, result, 0, keywords.length);
        System.arraycopy(extraKeywords, 0, result, keywords.length, extraKeywords.length);
        return result;
    }

    /**
     * 점수 섹션별 계산 결과를 나타내는 내부 객체
     * 섹션명, 점수, 최대 점수, 요약, 세부 항목 목록을 함께 가진다.
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }
    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record SectionResult(
            String section,
            double score,
            double maxScore,
            String summary,
            List<ScoreDetail> details
    ) {
        ScoreBreakdown toBreakdown() {
            return ScoreBreakdown.builder()
                    .section(section)
                    .score(score)
                    .maxScore(maxScore)
                    .summary(summary)
                    .details(details)
                    .build();
        }
    }


}
