package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.dto.ScoreBreakdown;
import com.team.meongnyang.recommendation.dto.ScoreDetail;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
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
 * 1차 필터를 통과한 후보를 "왜 더 적합한지" 설명 가능한 형태로 점수화한다.
 * 하드 필터는 OrchPlaceService에서 담당하고, 여기서는 상대적인 우선순위만 계산한다.
 *
 */
/**
 * 1차 필터를 통과한 후보 장소를 사용자 상황에 맞는 추천 순위로 재정렬하는 점수 계산기이다.
 *
 * <p>반려동물 적합도, 날씨 적합도, 장소 환경, 이동 편의성, 보너스 요소와 페널티를 조합해
 * 각 장소의 설명 가능한 점수를 만든다. 파이프라인 흐름에서는 후보 장소 수집 다음 단계에서 호출되며,
 * 계산 결과는 프롬프트 생성과 최종 추천 순위 설명에 직접 사용된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlaceScoringService {

    private static final double DEFAULT_USER_LAT = 37.27;
    private static final double DEFAULT_USER_LNG = 127.01;

    /**
     * 장소 점수 계산에 사용되는 상수들
     * - 펫 : 50.0
     * - 날씨 : 20.0
     * - 환경 : 15.0
     * - 이동성 : 10.0
     * - 보너스 : 5.0
     * - 총합 : 100.0
     */
    private static final double PET_MAX = 50.0;
    private static final double WEATHER_MAX = 20.0;
    private static final double ENVIRONMENT_MAX = 15.0;
    private static final double MOBILITY_MAX = 10.0;
    private static final double BONUS_MAX = 5.0;
    private static final double TOTAL_MAX = 100.0;

    private final DistanceCalculator distanceCalculator;

    public List<ScoredPlace> scorePlace(List<Place> candidates, User user, Pet pet, WeatherContext weather) {
        return scorePlaces(candidates, user, pet, weather);
    }

    /**
     * 사용자 좌표가 따로 주어지지 않은 경우 기본 좌표를 기준으로 후보 장소 점수를 계산한다.
     *
     * @param candidates 1차 필터를 통과한 후보 장소 목록
     * @param user 추천 대상 사용자 정보
     * @param pet 추천 기준이 되는 반려동물 정보
     * @param weather 현재 추천에 반영할 날씨 정보
     * @return 총점 내림차순으로 정렬된 장소 점수 목록
     */
    public List<ScoredPlace> scorePlaces(List<Place> candidates, User user, Pet pet, WeatherContext weather) {
        return scorePlaces(candidates, user, pet, weather, DEFAULT_USER_LAT, DEFAULT_USER_LNG);
    }

    /**
     * 후보 장소 전체를 순회하며 세부 점수를 계산하고 최종 추천 순위로 정렬한다.
     *
 * <p>파이프라인 흐름에서 후보 장소 수집 이후 호출되며,
     * 반환된 결과는 프롬프트의 Top 후보와 추천 근거 문장을 만드는 입력으로 사용된다.
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
            double userLng
    ) {
        // 장소가 없으면 빈 리스트를 반환합니다.
        if (candidates == null || candidates.isEmpty()) {
            log.warn("[장소 점수] 점수 계산 대상 후보가 없습니다.");
            return List.of();
        }

        // 후보 장소별 설명 가능한 점수를 계산한 뒤 총점 내림차순으로 정렬
        List<ScoredPlace> rankedPlaces = candidates.stream()
                .map(place -> scoreSinglePlace(place, user, pet, weather, userLat, userLng))
                .sorted(Comparator.comparingDouble(ScoredPlace::getTotalScore).reversed())
                .toList();
        log.info("[장소 점수] 상위 점수 결과 count={}, topPlaces={}",
                rankedPlaces.size(),
                rankedPlaces.stream()
                        .limit(3)
                        .map(scoredPlace -> scoredPlace.getPlace().getTitle() + "|" + scoredPlace.getTotalScore())
                        .collect(Collectors.joining(", ", "[", "]")));
        return rankedPlaces;
    }

    /**
     * 단일 장소의 세부 점수와 설명 문장을 계산해 최종 점수 객체로 변환한다.
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
            double userLng
    ) {
        // 1. 섹션별 점수 계산
        SectionResult petResult = scorePetSuitability(place, pet); // 장소에 반려동물이 적합한지 점수를 계산합니다.
        SectionResult weatherResult = scoreWeatherSuitability(place, weather); // 장소에 날씨가 적합한지 점수를 계산합니다.
        SectionResult environmentResult = scoreEnvironment(place, pet); // 환경 점수를 계산합니다.
        SectionResult mobilityResult = scoreMobility(place, user, userLat, userLng); // 이동성 점수를 계산합니다.
        SectionResult bonusResult = scoreBonus(place, pet); // 보너스 점수를 계산합니다.

        // 2. 조합상 불리한 조건에 대한 감점 반영
        double penaltyScore = applyPenaltyIfNeeded(place, pet, weather);
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

        log.info("[장소 점수] 장소={}, 총점={}, 반려동물={}, 날씨={}, 환경={}, 이동={}, 부가={}, 감점={}",
                place.getTitle(),
                totalScore,
                petResult.score(),
                weatherResult.score(),
                environmentResult.score(),
                mobilityResult.score(),
                bonusResult.score(),
                penaltyScore);
        logDebugScoreBreakdown(place, totalScore, penaltyScore, breakdowns);

        // 6. 최종 점수 결과 객체 반환
        return ScoredPlace.builder()
                .place(place)
                .totalScore(totalScore)
                .dogFitScore(petResult.score())
                .weatherScore(weatherResult.score())
                .placeEnvScore(environmentResult.score())
                .distanceScore(mobilityResult.score())
                .historyScore(bonusResult.score())
                .penaltyScore(penaltyScore)
                .sectionScores(sectionScores)
                .breakdowns(breakdowns)
                .scoreDetails(scoreDetails)
                .summary(summary)
                .reason(reason)
                .build();
    }

    /**
     * 장소에 반려동물이 적합한지 점수를 계산합니다.
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
     * 장소에 날씨가 적합한지 점수를 계산합니다.
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
     * 장소에 환경이 적합한지 점수를 계산합니다.
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
     * 장소에 이동 편의성이 적합한지 점수를 계산합니다.
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
     * 장소에 부가 요소가 적합한지 점수를 계산합니다.
     *
     * 본 점수 외에 추천 설명을 강화할 수 있는 보조 요소를 가산점으로 반영합니다.
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
            log.info("[장소 점수] 선호 장소 반영 place={}, preferredPlace={}, bonus={}",
                    place.getTitle(),
                    pet == null ? null : pet.getPreferredPlace(),
                    round(preferredPlaceScore));
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
            score = 2.4;
            reason = "대표견 선호 장소와 직접 일치하고 장소 성격도 유사합니다.";
        } else if (directMatch) {
            score = 2.0;
            reason = "대표견 선호 장소와 직접 일치하는 키워드가 감지되었습니다.";
        } else if (semanticMatch) {
            score = 1.5;
            reason = "대표견 선호 장소와 장소 카테고리/태그/설명이 유사하게 매칭되었습니다.";
        }

        if (score > 0.0) {
            details.add(detail("부가 요소", "대표견 선호 장소", score, BONUS_MAX, reason));
        }
        return score;
    }

    /**
     * 개별 항목 점수만으로 반영하기 어려운 조합 리스크를 감점으로 처리합니다.
     * 예: 노령견 + 활동적인 실외 장소, 비 오는 날 + 야외 장소
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
        boolean crowded = hasAnyKeyword(searchable, "붐빔", "혼잡", "페스티벌", "행사", "시장", "핫플");
        boolean activeOutdoor = hasAnyKeyword(searchable, "공원", "산책로", "운동장", "러닝", "잔디");

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

        return round(clamp(penalty, 0.0, 8.0));
    }

    /**
     * 점수를 정규화합니다. 최소 0, 최대 TOTAL_MAX 사이로 클램핑하고 소수점 1자리까지 반올림합니다.
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
            double score = hasAnyKeyword(searchable, "운동장", "공원", "잔디", "산책로") ? 10.5 : 7.5;
            details.add(detail("반려동물 적합도", "나이", score, 12.0, "어린 반려견은 활동 가능한 공간에서 강점을 가집니다."));
            return score;
        }
        if (age >= 10) {
            double score = hasAnyKeyword(searchable, "실내", "카페", "휴식", "조용") ? 10.0 : 6.5;
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
            case HIGH -> hasAnyKeyword(searchable, "공원", "산책로", "운동장", "야외") ? 11.0 : 6.5;
            case NORMAL -> 8.5;
            case LOW -> hasAnyKeyword(searchable, "실내", "카페", "휴식", "조용") ? 10.0 : 6.0;
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
            case LARGE -> hasAnyKeyword(searchable, "넓", "공원", "산책로", "운동장") ? 8.5 : 5.5;
            case MEDIUM -> 7.0;
            case SMALL -> hasAnyKeyword(searchable, "실내", "카페", "소형", "아늑") ? 8.0 : 6.5;
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
        if ((personality.contains("활발") || personality.contains("에너지")) && hasAnyKeyword(searchable, "공원", "산책로", "운동장")) {
            score += 1.5;
        }
        if ((personality.contains("예민") || personality.contains("소심") || personality.contains("겁")) && hasAnyKeyword(searchable, "조용", "실내", "아늑")) {
            score += 1.5;
        }
        if ((personality.contains("사교") || personality.contains("친화")) && hasAnyKeyword(searchable, "카페", "라운지", "동반")) {
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
        if ((breed.contains("리트리버") || breed.contains("보더") || breed.contains("허스키")) && hasAnyKeyword(searchable, "공원", "산책로")) {
            score += 1.0;
        }
        if ((breed.contains("말티즈") || breed.contains("푸들") || breed.contains("포메")) && hasAnyKeyword(searchable, "실내", "카페")) {
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
        if (hasAnyKeyword(searchable, "반려견", "반려동물", "동반", "펫", "애견")) {
            score += 4.5;
        }
        if (hasAnyKeyword(searchable, "주차", "휴식", "배변", "물", "테라스")) {
            score += 1.5;
        }

        score = clamp(score, 0.0, 6.0);
        details.add(detail("장소 환경 적합도", "동반 친화성", score, 6.0, "반려동물 동반 관련 태그와 설명을 반영했습니다."));
        return score;
    }

    private double scoreSafetyAndComfort(Place place, String searchable, List<ScoreDetail> details) {
        double score = 4.5;
        if (hasAnyKeyword(searchable, "넓", "쾌적", "조용", "안전")) {
            score += 1.5;
        }
        if (hasAnyKeyword(searchable, "혼잡", "붐빔", "행사장")) {
            score -= 1.0;
        }

        score = clamp(score, 0.0, 5.0);
        details.add(detail("장소 환경 적합도", "안전/쾌적성", score, 5.0, "안전성과 혼잡 가능성을 함께 반영했습니다."));
        return score;
    }

    private double scorePlaceQuality(Place place, List<ScoreDetail> details) {
        double rating = place.getRating() == null ? 0.0 : place.getRating();
        int reviewCount = place.getReviewCount() == null ? 0 : place.getReviewCount();

        double score = Math.min(2.5, rating * 0.5);
        if (reviewCount >= 100) {
            score += 1.5;
        } else if (reviewCount >= 30) {
            score += 1.0;
        } else if (reviewCount > 0) {
            score += 0.5;
        }

        score = clamp(score, 0.0, 4.0);
        details.add(detail("장소 환경 적합도", "품질 지표", score, 4.0, "평점과 리뷰 수를 보조 지표로 사용했습니다."));
        return score;
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
     * breakdowns List를  ScoreDetail List로 변환
     * @param breakdowns
     * @return
     */
    private List<ScoreDetail> flattenDetails(List<ScoreBreakdown> breakdowns) {
        return breakdowns.stream()
                .flatMap(breakdown -> breakdown.getDetails().stream())
                .toList();
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
        return hasAnyKeyword(searchable, "실내", "카페", "전시", "박물관", "라운지")
                || ("DINING".equalsIgnoreCase(place.getCategory()) && !hasAnyKeyword(searchable, "야외", "실외"));
    }

    private boolean isOutdoorPlace(Place place) {
        String searchable = searchablePlaceText(place);
        return hasAnyKeyword(searchable, "실외", "야외", "공원", "산책로", "운동장", "테라스");
    }

    private boolean matchesPreferredPlace(Place place, String searchable, String preferredPlace) {
        if (preferredPlace.contains("실내카페")) {
            return "DINING".equalsIgnoreCase(place.getCategory())
                    && hasAnyKeyword(searchable, "실내", "카페", "실내가능", "라운지", "휴식", "동반");
        }
        if (preferredPlace.contains("공원")) {
            return "PLACE".equalsIgnoreCase(place.getCategory())
                    && hasAnyKeyword(searchable, "공원", "야외", "실외", "산책로", "잔디", "테라스");
        }
        if (preferredPlace.contains("넓은 야외")) {
            return "PLACE".equalsIgnoreCase(place.getCategory())
                    && hasAnyKeyword(searchable, "넓", "야외", "실외", "공원", "산책로", "운동장", "잔디");
        }
        return false;
    }

    private boolean hasRichDescription(Place place) {
        return place.getDescription() != null && place.getDescription().trim().length() >= 40;
    }

    private boolean hasRichTags(Place place) {
        return place.getTags() != null && place.getTags().split(",").length >= 3;
    }

    private boolean isSensitivePet(Pet pet) {
        String personality = RecommendationTextUtils.normalizeTrimLower(pet.getPersonality());
        return personality.contains("예민") || personality.contains("소심") || personality.contains("겁");
    }

    private double smoothDistanceScore(double distanceKm) {
        double score = 10.0 / (1.0 + (distanceKm / 3.0));
        return round(clamp(score, 1.0, MOBILITY_MAX));
    }

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
                        RecommendationTextUtils.normalizeTrimLower(place.getTags())
                ).stream()
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

    /**
     * 유틸
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
