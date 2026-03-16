package com.team.meongnyang.orchestrator.service;

import com.team.meongnyang.orchestrator.dto.ScoreDetail;
import com.team.meongnyang.orchestrator.dto.ScoredPlace;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.weather.dto.WeatherContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlaceScoreService {

    private static final double DEFAULT_USER_LAT = 37.27;
    private static final double DEFAULT_USER_LNG = 127.01;

    private static final Set<String> INDOOR_KEYWORDS = Set.of("실내", "실내가능", "카페", "브런치");
    private static final Set<String> OUTDOOR_KEYWORDS = Set.of("야외", "실외", "공원", "산책로", "운동장", "잔디");
    private static final Set<String> QUIET_KEYWORDS = Set.of("조용", "한적", "프라이빗");
    private static final Set<String> ACTIVE_KEYWORDS = Set.of("산책", "운동", "활발", "에너지", "놀이", "뛰");
    private static final Set<String> ANXIOUS_KEYWORDS = Set.of("예민", "소심", "겁", "낯가림", "불안");
    private static final Set<String> SOCIAL_KEYWORDS = Set.of("사교", "사람", "친화", "외향");
    private static final Set<String> SENIOR_KEYWORDS = Set.of("노령", "시니어", "관절", "노견");
    private static final Set<String> PUPPY_KEYWORDS = Set.of("퍼피", "유견", "어린", "새끼");

    private static final Set<String> BRACHYCEPHALIC_BREEDS = Set.of("퍼그", "프렌치불독", "불독", "시츄", "페키니즈", "보스턴테리어");
    private static final Set<String> LARGE_ACTIVE_BREEDS = Set.of("골든리트리버", "래브라도리트리버", "저먼셰퍼드", "보더콜리", "시베리안허스키");
    private static final Set<String> SMALL_SENSITIVE_BREEDS = Set.of("말티즈", "치와와", "포메라니안", "요크셔테리어", "토이푸들");
    private static final Set<String> LONG_HAIR_BREEDS = Set.of("포메라니안", "골든리트리버", "사모예드", "시베리안허스키", "말티즈");
    private static final Set<String> POSITIVE_REVIEW_KEYWORDS = Set.of("청결", "친절", "넓", "쾌적", "안전", "추천", "휴식", "반려견", "동반");

    private final CalcDistanceService calcDistanceService;

    /**
     * 역할 : 후보 장소 목록 전체를 점수화하고, 총점 기준으로 정렬해서 반환
     *
     * @param candidates 후보 장소 목록
     * @param user 사용자 정보
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     * @return 점수 계산된 장소 목록
     */
    public List<ScoredPlace> scorePlace(List<Place> candidates, User user, Dog dog, WeatherContext weather) {
        // 입력 유효성 검사
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // 장소 점수 계산
        return candidates.stream()
                .map(place -> scoreSinglePlace(place, user, dog, weather))
                .sorted(Comparator.comparingDouble(ScoredPlace::getTotalScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 역할 : 장소 1개에 대해 전체 점수 계산을 수행하고 ScoredPlace로 변환
     *
     * @param place 장소 정보
     * @param user 사용자 정보
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     * @return 점수 계산된 장소 정보
     */
    private ScoredPlace scoreSinglePlace(Place place, User user, Dog dog, WeatherContext weather) {
        PlaceProfile profile = buildPlaceProfile(place);
        ScoreAccumulator accumulator = new ScoreAccumulator();

        evaluateDogSuitability(accumulator, profile, dog, weather);
        evaluateWeatherSuitability(accumulator, profile, dog, weather);
        evaluatePlaceEnvironment(accumulator, profile, dog, weather);
        evaluateDistanceAndMobility(accumulator, place, dog, weather);
        evaluateExtraFactors(accumulator, profile, dog, user);

        // 최종 점수 계산
        FinalizedScore finalized = accumulator.finalizeScores();
        log.info("[PLACE_SCORE] title={}, total={}, dog={}, weather={}, env={}, distance={}, extra={}",
                place.getTitle(),
                finalized.totalScore(),
                finalized.sectionScore(ScoreSection.DOG),
                finalized.sectionScore(ScoreSection.WEATHER),
                finalized.sectionScore(ScoreSection.PLACE_ENV),
                finalized.sectionScore(ScoreSection.DISTANCE),
                finalized.sectionScore(ScoreSection.EXTRA));


        return ScoredPlace.builder()
                .place(place)
                .totalScore(finalized.totalScore())
                .dogFitScore(finalized.sectionScore(ScoreSection.DOG))
                .weatherScore(finalized.sectionScore(ScoreSection.WEATHER))
                .placeEnvScore(finalized.sectionScore(ScoreSection.PLACE_ENV))
                .distanceScore(finalized.sectionScore(ScoreSection.DISTANCE))
                .historyScore(finalized.sectionScore(ScoreSection.EXTRA))
                .sectionScores(finalized.sectionScores())
                .scoreDetails(finalized.details())
                .reason(buildReasonSummary(finalized))
                .build();
    }

    /**
     * 역할 : 반려견 적합도 섹션 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void evaluateDogSuitability(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        addDogAgeScore(accumulator, profile, dog);
        addBreedSpecificScore(accumulator, profile, dog, weather);
        addPersonalityScore(accumulator, profile, dog);
        addHealthAndCautionScore(accumulator, profile, dog, weather);
        addPreferredPlaceScore(accumulator, profile, dog);
    }

    /**
     * 역할 : 날씨 적합도 섹션 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void evaluateWeatherSuitability(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        if (!isWeatherAvailable(weather)) {
            accumulator.skipSection(ScoreSection.WEATHER, "날씨 정보가 없어 날씨 적합도는 총점 정규화에서 제외했습니다.");
            return;
        }

        addTemperatureScore(accumulator, profile, dog, weather);
        addPrecipitationScore(accumulator, profile, weather);
        addHumidityScore(accumulator, profile, dog, weather);
        addWindScore(accumulator, profile, dog, weather);
        addWeatherRiskScore(accumulator, profile, dog, weather);
    }

    /**
     * 역할 : 장소 환경 섹션 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void evaluatePlaceEnvironment(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        addIndoorOutdoorScore(accumulator, profile, dog, weather);
        addCategoryScore(accumulator, profile, dog);
        addSafetyScore(accumulator, profile, dog);
        addCrowdScore(accumulator, profile, dog);
        addDogFriendlyScore(accumulator, profile, dog);
    }

    /**
     * 역할 : 장소 접근성 섹션 점수 계산
     * @param accumulator 점수 계산기
     * @param place 장소 정보
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void evaluateDistanceAndMobility(ScoreAccumulator accumulator, Place place, Dog dog, WeatherContext weather) {
        double distanceKm = calculateDistanceKm(place);
        addDistanceScore(accumulator, distanceKm);
        addTravelBurdenScore(accumulator, place, dog, weather, distanceKm);
    }

    /**
     * 역할 : 추가적인 장소 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param user 사용자 정보
     */
    private void evaluateExtraFactors(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, User user) {
        addReviewKeywordScore(accumulator, profile);
        addRevisitPotentialScore(accumulator, profile, dog);
        addGuardianSatisfactionScore(accumulator, profile, user);
    }

    /**
     * 역할 : 장소 내부/외부 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addIndoorOutdoorScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        double score = 2.5;
        List<String> reasons = new ArrayList<>();

        if (profile.hasAny(INDOOR_KEYWORDS) && profile.hasAny(OUTDOOR_KEYWORDS)) {
            score = 5.0;
            reasons.add("실내/실외 선택지가 모두 있어 상황 대응력이 높습니다.");
        } else if (profile.hasAny(INDOOR_KEYWORDS)) {
            score = weather != null && !"GOOD".equalsIgnoreCase(weather.getWalkLevel()) ? 4.5 : 3.5;
            reasons.add("실내 위주라 날씨 리스크 대응이 쉽습니다.");
        } else if (profile.hasAny(OUTDOOR_KEYWORDS)) {
            score = weather != null && "GOOD".equalsIgnoreCase(weather.getWalkLevel()) ? 4.5 : 2.0;
            reasons.add("야외 위주라 날씨가 좋을 때 장점이 커집니다.");
        }

        if (dog != null && dog.getDogSize() == Dog.DogSize.LARGE && profile.hasKeyword("운동장")) {
            score += 0.5;
            reasons.add("대형견이 활동하기 좋은 야외 공간이 있습니다.");
        }

        accumulator.addDetail(ScoreSection.PLACE_ENV, "실내/실외", 5.0, clamp(score, 0.0, 5.0),
                joinReasons(reasons, "공간 유형 정보가 제한적이라 중립 점수를 유지했습니다."));
    }

    /**
     * 역할 : 장소 카테고리 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addCategoryScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        String category = normalizeText(profile.category());
        double score = 2.0;
        List<String> reasons = new ArrayList<>();

        if (category.contains("place")) {
            score = profile.hasAny(OUTDOOR_KEYWORDS) ? 4.0 : 3.0;
            reasons.add("플레이스 카테고리는 산책/체류형 활동과의 연결성이 좋습니다.");
        } else if (category.contains("dining")) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 3.5 : 2.5;
            reasons.add("다이닝 카테고리는 실내 휴식형 니즈에 강점이 있습니다.");
        } else if (category.contains("stay")) {
            score = 3.5;
            reasons.add("스테이는 장시간 체류 관점에서 안정성이 있습니다.");
        }

        if (dog != null && dog.getPreferredPlace() != null && profile.matchesPreference(normalizeText(dog.getPreferredPlace()))) {
            score += 0.5;
            reasons.add("반려견 선호 장소와 카테고리 방향이 맞습니다.");
        }

        accumulator.addDetail(ScoreSection.PLACE_ENV, "카테고리", 4.0, clamp(score, 0.0, 4.0),
                joinReasons(reasons, "카테고리 기반 기본 적합도를 반영했습니다."));
    }

    /**
     * 역할 : 장소 안전성 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addSafetyScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        double score = 1.5;
        List<String> reasons = new ArrayList<>();

        if (profile.hasKeyword("주차가능")) {
            score += 1.0;
            reasons.add("주차 가능으로 하차/이동 구간의 안전성과 편의성이 좋습니다.");
        }
        if (profile.hasKeyword("대형견가능") && dog != null && dog.getDogSize() == Dog.DogSize.LARGE) {
            score += 1.0;
            reasons.add("대형견 허용 정보가 명시돼 있습니다.");
        }
        if (profile.hasKeyword("소형견가능") && dog != null && dog.getDogSize() == Dog.DogSize.SMALL) {
            score += 1.0;
            reasons.add("소형견 허용 정보가 명시돼 있습니다.");
        }
        if (profile.rating() >= 4.5) {
            score += 0.8;
            reasons.add("평점이 높아 전반적 운영 안정성을 긍정적으로 봤습니다.");
        }
        if (profile.phoneAvailable() && profile.imageAvailable()) {
            score += 0.7;
            reasons.add("연락처와 이미지가 있어 기본 정보 신뢰도가 높습니다.");
        }

        accumulator.addDetail(ScoreSection.PLACE_ENV, "안전성", 4.0, clamp(score, 0.0, 4.0),
                joinReasons(reasons, "안전 관련 명시 태그가 적어 기본점수 위주로 반영했습니다."));
    }

    /**
     * 역할 : 장소 혼잡도 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addCrowdScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        double score = 2.0;
        List<String> reasons = new ArrayList<>();

        if (!profile.estimatedCrowdHigh()) {
            score = 2.5;
            reasons.add("리뷰 수 기준으로 과도한 혼잡 가능성은 낮다고 판단했습니다.");
        } else {
            reasons.add("리뷰 수가 많아 시간대에 따라 혼잡할 가능성을 반영했습니다.");
            if (dog != null && dog.getPersonality() != null && containsAny(normalizeText(dog.getPersonality()), ANXIOUS_KEYWORDS)) {
                score = 0.8;
                reasons.add("예민한 성향의 반려견에게는 혼잡도가 불리합니다.");
            } else if (dog != null && dog.getPersonality() != null && containsAny(normalizeText(dog.getPersonality()), SOCIAL_KEYWORDS)) {
                score = 2.5;
                reasons.add("사회성이 높은 성향이면 혼잡 리스크가 일부 상쇄됩니다.");
            } else {
                score = 1.5;
            }
        }

        accumulator.addDetail(ScoreSection.PLACE_ENV, "혼잡도", 3.0, clamp(score, 0.0, 3.0),
                joinReasons(reasons, "혼잡도 정보가 제한적이라 리뷰 수를 대리 지표로 사용했습니다."));
    }

    /**
     * 반려견 친화도 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addDogFriendlyScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        double score = 1.5;
        List<String> reasons = new ArrayList<>();

        if (profile.hasKeyword("반려견") || profile.hasKeyword("동반")) {
            score += 1.5;
            reasons.add("반려견 동반 문맥이 명시돼 있습니다.");
        }
        if (profile.hasKeyword("운동장") || profile.hasKeyword("산책로")) {
            score += 1.0;
            reasons.add("반려견 활동을 위한 공간 정보가 있습니다.");
        }
        if (dog != null && dog.getDogSize() == Dog.DogSize.LARGE && profile.hasKeyword("대형견가능")) {
            score += 0.8;
            reasons.add("대형견 허용 태그가 직접 일치합니다.");
        }
        if (dog != null && dog.getDogSize() == Dog.DogSize.SMALL && profile.hasKeyword("소형견가능")) {
            score += 0.8;
            reasons.add("소형견 허용 태그가 직접 일치합니다.");
        }

        accumulator.addDetail(ScoreSection.PLACE_ENV, "반려견 친화성", 4.0, clamp(score, 0.0, 4.0),
                joinReasons(reasons, "반려견 친화 정보가 제한적이라 기본 보정을 적용했습니다."));
    }

    /**
     * 장소와 사용자 사이의 거리 계산
     * @param place 장소 정보
     * @return 거리(km) 또는 -1.0 (좌표 정보 부족)
     */
    private double calculateDistanceKm(Place place) {
        if (place.getLatitude() == null || place.getLongitude() == null) {
            return -1.0;
        }

        return calcDistanceService.calculateDistanceKm(DEFAULT_USER_LAT, DEFAULT_USER_LNG, place.getLatitude(), place.getLongitude());
    }

    /**
     * 사용자와 장소 사이의 거리 점수 계산
     * @param accumulator 점수 계산기
     * @param distanceKm 거리(km)
     */
    private void addDistanceScore(ScoreAccumulator accumulator, double distanceKm) {
        if (distanceKm < 0) {
            accumulator.addDetail(ScoreSection.DISTANCE, "사용자 위치와 장소 거리", 6.0, 1.0, "장소 좌표가 부족해 거리 계산을 보수적으로 처리했습니다.");
            return;
        }

        double score;
        String reason;

        if (distanceKm <= 2.0) {
            score = 6.0;
            reason = "2km 이내라 즉흥 외출에도 부담이 적습니다.";
        } else if (distanceKm <= 5.0) {
            score = 5.0;
            reason = "가까운 편이라 왕복 이동 부담이 크지 않습니다.";
        } else if (distanceKm <= 10.0) {
            score = 3.5;
            reason = "중거리 이동으로 관리 가능한 수준입니다.";
        } else if (distanceKm <= 20.0) {
            score = 2.0;
            reason = "장거리 편이라 짧은 외출 목적에는 다소 불리합니다.";
        } else {
            score = 0.5;
            reason = "상당히 먼 거리라 이동 효율 측면에서 큰 감점을 적용했습니다.";
        }

        accumulator.addDetail(ScoreSection.DISTANCE, "사용자 위치와 장소 거리", 6.0, score,
                reason + " (계산 거리: " + round(distanceKm) + "km)");
    }

    /**
     * 이동 부담 점수 계산
     * @param accumulator 점수 계산기
     * @param place 장소 정보
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     * @param distanceKm 거리(km)
     */
    private void addTravelBurdenScore(ScoreAccumulator accumulator, Place place, Dog dog, WeatherContext weather, double distanceKm) {
        double score = 2.5;
        List<String> reasons = new ArrayList<>();

        if (distanceKm < 0) {
            score = 1.5;
            reasons.add("거리 정보가 불완전해 이동 부담은 보수적으로 계산했습니다.");
        } else if (distanceKm <= 3.0) {
            score = 4.0;
            reasons.add("짧은 이동으로 반려견 컨디션 소모가 적습니다.");
        } else if (distanceKm > 10.0) {
            score = 1.5;
            reasons.add("이동 시간이 길어 외출 피로도 상승 가능성을 반영했습니다.");
        }
        if (weather != null && !"GOOD".equalsIgnoreCase(weather.getWalkLevel()) && distanceKm > 5.0) {
            score -= 1.0;
            reasons.add("날씨가 좋지 않은데 이동 거리까지 길어 추가 감점했습니다.");
        }
        if (dog != null && dog.getDogSize() == Dog.DogSize.SMALL && distanceKm > 7.0) {
            score -= 0.5;
            reasons.add("소형견의 장거리 이동 부담을 반영했습니다.");
        }
        if (dog != null && dog.getPersonality() != null && containsAny(normalizeText(dog.getPersonality()), ANXIOUS_KEYWORDS) && distanceKm > 5.0) {
            score -= 0.5;
            reasons.add("예민한 성향의 이동 스트레스를 추가로 감점했습니다.");
        }
        if ("STAY".equalsIgnoreCase(place.getCategory()) && distanceKm > 10.0) {
            score += 0.8;
            reasons.add("숙박형 장소는 장거리 이동의 상대적 부담을 일부 상쇄합니다.");
        }

        accumulator.addDetail(ScoreSection.DISTANCE, "이동 부담", 4.0, clamp(score, 0.0, 4.0),
                joinReasons(reasons, "이동 부담은 중립값에서 시작해 거리와 날씨로 보정했습니다."));
    }

    /**
     * 리뷰 키워드 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     */
    private void addReviewKeywordScore(ScoreAccumulator accumulator, PlaceProfile profile) {
        int matchedKeywordCount = countMatches(profile.corpus(), POSITIVE_REVIEW_KEYWORDS);
        double score = 1.0 + Math.min(matchedKeywordCount * 0.6, 2.0);
        String reason = matchedKeywordCount > 0
                ? "설명/태그에서 긍정 키워드가 " + matchedKeywordCount + "개 감지돼 리뷰 품질 기대치를 가점했습니다."
                : "리뷰 키워드 정보가 많지 않아 기본점수 위주로 반영했습니다.";

        if (profile.rating() >= 4.3) {
            score += 0.8;
            reason += " 평점도 높아 추가 가점을 적용했습니다.";
        }
        if (profile.reviewCount() >= 50) {
            score += 0.4;
            reason += " 리뷰 수가 적정 수준 이상이라 신뢰도를 높게 봤습니다.";
        }

        accumulator.addDetail(ScoreSection.EXTRA, "리뷰 키워드", 4.0, clamp(score, 0.0, 4.0), reason);
    }

    /**
     * 재방문 가능성 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addRevisitPotentialScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        double score = 1.2;
        List<String> reasons = new ArrayList<>();

        if (profile.rating() >= 4.0) {
            score += 0.8;
            reasons.add("평점이 안정적이라 재방문 후보로 보기 좋습니다.");
        }
        if (dog != null && dog.getPreferredPlace() != null && profile.matchesPreference(normalizeText(dog.getPreferredPlace()))) {
            score += 0.8;
            reasons.add("선호 장소와 맞아 재방문 가능성이 높습니다.");
        }
        if (profile.hasKeyword("주차가능")) {
            score += 0.4;
            reasons.add("재방문 시에도 이용 동선이 편합니다.");
        }

        accumulator.addDetail(ScoreSection.EXTRA, "재방문 가능성", 3.0, clamp(score, 0.0, 3.0),
                joinReasons(reasons, "재방문 지표가 부족해 기본 보정을 적용했습니다."));
    }

    /**
     * 보호자 만족도 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param user 사용자 정보
     */
    private void addGuardianSatisfactionScore(ScoreAccumulator accumulator, PlaceProfile profile, User user) {
        double score = 1.0;
        List<String> reasons = new ArrayList<>();

        if (profile.phoneAvailable()) {
            score += 0.7;
            reasons.add("연락처가 있어 방문 전 확인이 쉽습니다.");
        }
        if (profile.imageAvailable()) {
            score += 0.4;
            reasons.add("대표 이미지가 있어 장소 판단이 편합니다.");
        }
        if (profile.addressAvailable()) {
            score += 0.4;
            reasons.add("주소 정보가 명확합니다.");
        }
        if (profile.hasKeyword("주차가능")) {
            score += 0.5;
            reasons.add("보호자 입장에서 주차 편의성이 큽니다.");
        }
        if (user != null && user.getStatus() != User.Status.ACTIVE) {
            score -= 0.5;
            reasons.add("비정상 계정 상태에서는 추천 신뢰도를 보수적으로 처리했습니다.");
        }

        accumulator.addDetail(ScoreSection.EXTRA, "보호자 만족 요소", 3.0, clamp(score, 0.0, 3.0),
                joinReasons(reasons, "장소 메타데이터 중심으로 보호자 만족도를 계산했습니다."));
    }

    /**
     * 반려견 나이 점수 계산
     *
     * todo : 나이에 따른 점수 계산 로직 추가 예정
     * Dog 엔티티 -> Pet 엔티티로 변경
     * 필드변경
     * | pet_id | BIGINT PK | 반려동물 고유 식별자 | PK, 자동 생성 |
     * | user_id | BIGINT FK | 소유 회원 ID | 필수, FK (삭제된 회원 등록 불가) |
     * | pet_name | VARCHAR(20) | 반려동물 이름 | 필수, 1~20자 |
     * | pet_type | VARCHAR | 종류 | 필수, `강아지` \| `고양이` |
     * | pet_breed | VARCHAR(50) | 품종 | 필수, 1~50자 |
     * | pet_gender | VARCHAR | 성별 | 필수, `남아` \| `여아` |
     * | pet_size | VARCHAR | 크기 | 필수, ENUM `SMALL` \| `MEDIUM` \| `LARGE` |
     * | pet_age | INT | 나이 (년) | 필수, 양수 |
     * | pet_weight | DECIMAL | 체중 (kg) | 선택, 양수 |
     * | pet_activity | VARCHAR | 활동량 | 필수, ENUM `LOW` \| `NORMAL` \| `HIGH` |
     * | personality | VARCHAR(100) | 성격 | 선택, 최대 100자 |
     * | preferred_place | VARCHAR(50) | 선호 장소 유형 | 선택, 최대 50자 |
     * | is_representative | BOOLEAN | 대표 동물 여부 (알림 수신 기준) | 기본값 false, 첫 등록 시 자동 true |
     * | reg_date | DATETIME | 등록일 | 자동 저장 |
     *
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addDogAgeScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        if (dog == null) {
            accumulator.addDetail(ScoreSection.DOG, "나이", 8.0, 3.0, "반려견 정보가 없어 나이 적합도는 보수적으로 반영했습니다.");
            return;
        }

        DogStage stage = inferDogStage(dog);
        double score;
        String reason;

        if (stage == DogStage.PUPPY) {
            if (profile.hasAny(OUTDOOR_KEYWORDS) || profile.hasKeyword("운동장")) {
                score = 7.5;
                reason = "어린 반려견으로 추정되어 활동량을 해소할 수 있는 공간을 높게 평가했습니다.";
            } else if (profile.hasAny(INDOOR_KEYWORDS)) {
                score = 5.0;
                reason = "안전성은 좋지만 활동량 해소 측면에서는 중립으로 반영했습니다.";
            } else {
                score = 4.0;
                reason = "정확한 나이 필드는 없어서 활동성 추정치만 반영했습니다.";
            }
        } else if (stage == DogStage.SENIOR) {
            if (profile.hasAny(INDOOR_KEYWORDS) || profile.hasAny(QUIET_KEYWORDS) || profile.hasKeyword("주차가능")) {
                score = 8.0;
                reason = "시니어 성향으로 추정되어 조용하고 이동 부담이 적은 환경을 높게 봤습니다.";
            } else if (profile.hasAny(OUTDOOR_KEYWORDS)) {
                score = 3.0;
                reason = "시니어 성향인데 야외 비중이 높아 체력 소모 가능성을 감점했습니다.";
            } else {
                score = 5.0;
                reason = "시니어 추정이지만 환경 정보가 제한적이라 보수적으로 반영했습니다.";
            }
        } else {
            score = profile.hasAny(INDOOR_KEYWORDS) && profile.hasAny(OUTDOOR_KEYWORDS) ? 7.0 : 6.0;
            reason = stage == DogStage.UNKNOWN
                    ? "정확한 나이 필드가 없어 성견 기준의 중립 점수로 처리했습니다."
                    : "성견 기준으로 무난한 활동 범위를 반영했습니다.";
        }

        accumulator.addDetail(ScoreSection.DOG, "나이", 8.0, score, reason);
    }

    /**
     * 반려견 품종 특이사항 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addBreedSpecificScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        if (dog == null || dog.getDogBreed() == null || dog.getDogBreed().isBlank()) {
            accumulator.addDetail(ScoreSection.DOG, "품종 특이사항", 7.0, 3.5, "견종 정보가 없어 기본 적합도로 반영했습니다.");
            return;
        }

        String breed = normalizeText(dog.getDogBreed());
        double score = 3.0;
        List<String> reasons = new ArrayList<>();

        if (containsBreed(breed, BRACHYCEPHALIC_BREEDS)) {
            score += 2.0;
            reasons.add("단두종 계열의 호흡 부담 가능성을 고려했습니다.");
            if (profile.hasAny(INDOOR_KEYWORDS)) {
                score += 1.5;
                reasons.add("실내 위주 환경이 단두종에 유리합니다.");
            }
            if (weather != null && (weather.isHot() || weather.getHumidity() >= 80) && profile.hasAny(OUTDOOR_KEYWORDS)) {
                score -= 2.5;
                reasons.add("고온다습한 야외 환경은 단두종에 불리합니다.");
            }
        }

        if (containsBreed(breed, LARGE_ACTIVE_BREEDS) || dog.getDogSize() == Dog.DogSize.LARGE) {
            if (profile.hasKeyword("대형견가능") || profile.hasKeyword("운동장") || profile.hasAny(OUTDOOR_KEYWORDS)) {
                score += 2.5;
                reasons.add("대형견/활동성 높은 견종이 활용하기 좋은 공간 요소가 있습니다.");
            } else {
                score -= 1.5;
                reasons.add("대형견 기준으로 공간 여유 정보가 부족합니다.");
            }
        }

        if (containsBreed(breed, SMALL_SENSITIVE_BREEDS) || dog.getDogSize() == Dog.DogSize.SMALL) {
            if (profile.hasAny(INDOOR_KEYWORDS) || profile.hasAny(QUIET_KEYWORDS) || profile.hasKeyword("소형견가능")) {
                score += 2.0;
                reasons.add("소형견이 안정적으로 머물기 좋은 조건입니다.");
            }
            if (weather != null && weather.isWindy() && profile.hasAny(OUTDOOR_KEYWORDS)) {
                score -= 1.5;
                reasons.add("강풍일 때 소형견의 야외 이동 부담을 감점했습니다.");
            }
        }

        accumulator.addDetail(ScoreSection.DOG, "품종 특이사항", 7.0, clamp(score, 0.0, 7.0),
                joinReasons(reasons, "견종/체형 특성에 맞춘 기본 보정치를 적용했습니다."));
    }

    /**
     * 반려견 성향 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addPersonalityScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        if (dog == null || dog.getPersonality() == null || dog.getPersonality().isBlank()) {
            accumulator.addDetail(ScoreSection.DOG, "성향", 7.0, 3.5, "성향 정보가 없어 중립 점수를 부여했습니다.");
            return;
        }

        String personality = normalizeText(dog.getPersonality());
        double score = 3.0;
        List<String> reasons = new ArrayList<>();

        if (containsAny(personality, ACTIVE_KEYWORDS)) {
            if (profile.hasAny(OUTDOOR_KEYWORDS) || profile.hasKeyword("운동장")) {
                score += 3.0;
                reasons.add("활동적인 성향과 야외/운동장 환경의 궁합이 좋습니다.");
            } else {
                score += 1.0;
                reasons.add("활동적인 성향이지만 공간 정보상 에너지 해소 요소는 제한적입니다.");
            }
        }

        if (containsAny(personality, ANXIOUS_KEYWORDS)) {
            if (profile.hasAny(QUIET_KEYWORDS) || profile.hasAny(INDOOR_KEYWORDS)) {
                score += 2.5;
                reasons.add("예민한 성향에 맞춰 조용하고 통제 가능한 환경을 가점했습니다.");
            }
            if (profile.estimatedCrowdHigh()) {
                score -= 2.0;
                reasons.add("혼잡 가능성이 있어 예민한 성향에는 감점했습니다.");
            }
        }

        if (containsAny(personality, SOCIAL_KEYWORDS) && profile.estimatedCrowdHigh()) {
            score += 1.5;
            reasons.add("사회성이 높아 혼잡 리스크가 일부 상쇄됩니다.");
        }

        accumulator.addDetail(ScoreSection.DOG, "성향", 7.0, clamp(score, 0.0, 7.0),
                joinReasons(reasons, "성향 키워드를 기준으로 무난한 적합도를 반영했습니다."));
    }

    /**
     * 반려견 건강과 주의 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addHealthAndCautionScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        double score = 3.5;
        List<String> reasons = new ArrayList<>();

        if (profile.hasKeyword("주차가능")) {
            score += 1.5;
            reasons.add("주차 가능으로 이동 부담을 줄일 수 있습니다.");
        }
        if (profile.hasAny(INDOOR_KEYWORDS)) {
            score += 1.0;
            reasons.add("실내 요소가 있어 컨디션이 좋지 않은 날에도 대응이 쉽습니다.");
        }
        if (profile.hasKeyword("운동장") || profile.hasKeyword("산책로")) {
            score += 0.8;
            reasons.add("통제 가능한 활동 동선이 있습니다.");
        }
        if (weather != null && weather.isRaining() && profile.hasAny(OUTDOOR_KEYWORDS) && !profile.hasAny(INDOOR_KEYWORDS)) {
            score -= 2.0;
            reasons.add("비 오는 날 야외 비중이 높아 컨디션 관리 측면에서 불리합니다.");
        }
        if (weather != null && weather.isHot() && isHeatSensitive(dog) && profile.hasAny(OUTDOOR_KEYWORDS)) {
            score -= 2.0;
            reasons.add("더위 취약 가능성이 있는 반려견에게 야외 위주 장소라 감점했습니다.");
        }
        if (weather != null && weather.isCold() && isColdSensitive(dog) && profile.hasAny(OUTDOOR_KEYWORDS)) {
            score -= 1.5;
            reasons.add("추위 취약 가능성이 있는 반려견의 야외 체류 부담을 반영했습니다.");
        }

        accumulator.addDetail(ScoreSection.DOG, "건강/주의사항", 7.0, clamp(score, 0.0, 7.0),
                joinReasons(reasons, "명시적인 건강 필드가 없어 환경 위험도 중심으로 계산했습니다."));
    }

    /**
     * 반려견 선호 장소 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     */
    private void addPreferredPlaceScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog) {
        if (dog == null || dog.getPreferredPlace() == null || dog.getPreferredPlace().isBlank()) {
            accumulator.addDetail(ScoreSection.DOG, "선호 장소", 6.0, 3.0, "선호 장소 정보가 없어 중립 점수로 처리했습니다.");
            return;
        }

        String preferred = normalizeText(dog.getPreferredPlace());
        double score = 2.0;
        List<String> reasons = new ArrayList<>();

        if (profile.matchesPreference(preferred)) {
            score += 3.5;
            reasons.add("선호 장소 키워드와 장소 카테고리/설명이 직접적으로 맞습니다.");
        }
        if (profile.hasKeyword("반려견") || profile.hasKeyword("동반")) {
            score += 0.5;
            reasons.add("반려견 동반 문맥이 명확합니다.");
        }
        if (preferred.contains("공원") && profile.hasAny(OUTDOOR_KEYWORDS)) {
            score += 1.0;
            reasons.add("공원/야외 선호와 실제 장소 성격이 일치합니다.");
        }

        accumulator.addDetail(ScoreSection.DOG, "선호 장소", 6.0, clamp(score, 0.0, 6.0),
                joinReasons(reasons, "선호 장소와의 직접 매칭이 약해 기본점수 위주로 반영했습니다."));
    }

    /**
     * 날씨에 따른 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addTemperatureScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        double temp = weather.getTemperature();
        double score;
        String reason;

        if (temp >= 12 && temp <= 22) {
            score = profile.hasAny(OUTDOOR_KEYWORDS) ? 8.0 : 6.5;
            reason = "기온이 안정적이라 야외 활동성 장소에 유리합니다.";
        } else if (temp > 28) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 7.0 : 2.0;
            reason = profile.hasAny(INDOOR_KEYWORDS) ? "고온 환경이라 실내형 장소를 높게 평가했습니다." : "고온 환경에서 야외 비중이 높아 큰 감점을 적용했습니다.";
            if (isHeatSensitive(dog) && profile.hasAny(OUTDOOR_KEYWORDS)) {
                score -= 1.5;
                reason += " 더위 민감 가능성도 추가 반영했습니다.";
            }
        } else if (temp < 5) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 7.0 : 2.5;
            reason = profile.hasAny(INDOOR_KEYWORDS) ? "저온 환경이라 실내형 장소가 적합합니다." : "저온 환경에서 야외 체류 부담이 커 감점했습니다.";
            if (isColdSensitive(dog) && profile.hasAny(OUTDOOR_KEYWORDS)) {
                score -= 1.0;
                reason += " 추위 민감 가능성도 반영했습니다.";
            }
        } else {
            score = 5.5;
            reason = "기온이 극단적이지는 않지만 실내/실외 선택에 주의가 필요한 구간입니다.";
        }

        accumulator.addDetail(ScoreSection.WEATHER, "기온", 8.0, clamp(score, 0.0, 8.0), reason);
    }

    /**
     * 강수량에 따른 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param weather 날씨 정보
     */
    private void addPrecipitationScore(ScoreAccumulator accumulator, PlaceProfile profile, WeatherContext weather) {
        double score;
        String reason;

        if (weather.isRaining() || weather.getRainfall() > 0.0) {
            if (profile.hasAny(INDOOR_KEYWORDS)) {
                score = weather.getRainfall() >= 5.0 ? 6.0 : 5.5;
                reason = "강수 상황이라 실내 장소를 우선 가점했습니다.";
            } else {
                score = weather.getRainfall() >= 5.0 ? 0.5 : 1.5;
                reason = "비가 오는데 야외 비중이 높아 크게 감점했습니다.";
            }
        } else {
            score = profile.hasAny(OUTDOOR_KEYWORDS) ? 5.5 : 4.0;
            reason = profile.hasAny(OUTDOOR_KEYWORDS) ? "무강수 조건에서 야외 장소 활용성이 좋습니다." : "실내 장소라 강수 측면의 추가 이점은 제한적입니다.";
        }

        accumulator.addDetail(ScoreSection.WEATHER, "강수 여부", 6.0, clamp(score, 0.0, 6.0), reason);
    }

    /**
     * 습도에 따른 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addHumidityScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        int humidity = weather.getHumidity();
        double score = 2.5;
        List<String> reasons = new ArrayList<>();

        if (humidity <= 70) {
            score = 4.0;
            reasons.add("습도가 비교적 안정적이라 활동 부담이 적습니다.");
        } else if (humidity >= 85) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 3.5 : 1.0;
            reasons.add("고습 환경이라 야외 체류 부담을 감점했습니다.");
            if (isHeatSensitive(dog) || isLongHairBreed(dog)) {
                score -= 0.8;
                reasons.add("더위/피모 특성상 고습 민감 가능성을 추가 반영했습니다.");
            }
        }

        accumulator.addDetail(ScoreSection.WEATHER, "습도", 4.0, clamp(score, 0.0, 4.0),
                joinReasons(reasons, "습도 영향은 중간 가중치로 반영했습니다."));
    }

    /**
     * 바람에 따른 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addWindScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        double windSpeed = weather.getWindSpeed();
        double score = 2.0;
        List<String> reasons = new ArrayList<>();

        if (windSpeed < 4.0) {
            score = 3.0;
            reasons.add("풍속이 낮아 이동 스트레스가 적습니다.");
        } else if (windSpeed >= 8.0) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 2.5 : 0.5;
            reasons.add("강풍 상황이라 야외 노출 장소를 크게 감점했습니다.");
            if (dog != null && dog.getDogSize() == Dog.DogSize.SMALL && profile.hasAny(OUTDOOR_KEYWORDS)) {
                score -= 0.5;
                reasons.add("소형견의 체감 부담을 추가 반영했습니다.");
            }
        }

        accumulator.addDetail(ScoreSection.WEATHER, "풍속", 3.0, clamp(score, 0.0, 3.0),
                joinReasons(reasons, "풍속 정보에 따른 기본 보정을 적용했습니다."));
    }

    /**
     * 날씨 위험도에 따른 점수 계산
     * @param accumulator 점수 계산기
     * @param profile 장소 프로필
     * @param dog 반려견 정보
     * @param weather 날씨 정보
     */
    private void addWeatherRiskScore(ScoreAccumulator accumulator, PlaceProfile profile, Dog dog, WeatherContext weather) {
        String walkLevel = normalizeText(weather.getWalkLevel());
        double score = 2.0;
        List<String> reasons = new ArrayList<>();

        if (walkLevel.contains("danger")) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 2.0 : 0.0;
            reasons.add("산책 위험 단계가 높아 야외 장소는 사실상 배제 수준으로 감점했습니다.");
        } else if (walkLevel.contains("caution")) {
            score = profile.hasAny(INDOOR_KEYWORDS) ? 4.0 : 1.5;
            reasons.add("주의 단계라 실내/짧은 동선 장소에 보정치를 줬습니다.");
        } else if (walkLevel.contains("good")) {
            score = profile.hasAny(OUTDOOR_KEYWORDS) ? 4.0 : 3.0;
            reasons.add("산책 가능 수준이 양호해 야외 활용성을 가점했습니다.");
        }

        if ((weather.isHot() || weather.isCold() || weather.isWindy()) && dog != null && dog.getDogSize() == Dog.DogSize.SMALL) {
            score -= 0.5;
            reasons.add("소형견의 체감 위험도를 추가로 감점했습니다.");
        }

        accumulator.addDetail(ScoreSection.WEATHER, "체감 위험 요소", 4.0, clamp(score, 0.0, 4.0),
                joinReasons(reasons, "산책 가능 수준 기반의 기본 보정을 적용했습니다."));
    }

    /**
     * 최종 점수 요약 메시지 생성
     * @param finalized 최종 점수 객체
     * @return 요약된 메시지 문자열
     */
    private String buildReasonSummary(FinalizedScore finalized) {
        List<String> messages = new ArrayList<>();

        finalized.details().stream()
                .filter(detail -> detail.getMaxScore() > 0 && detail.getScore() >= detail.getMaxScore() * 0.75)
                .limit(3)
                .forEach(detail -> messages.add(detail.getSection() + " - " + detail.getItem() + " 강점"));

        finalized.details().stream()
                .filter(detail -> detail.getMaxScore() > 0 && detail.getScore() <= detail.getMaxScore() * 0.35)
                .limit(2)
                .forEach(detail -> messages.add(detail.getSection() + " - " + detail.getItem() + " 주의"));

        if (messages.isEmpty()) {
            messages.add("전반적으로 큰 약점 없이 무난한 추천 후보입니다.");
        }

        return String.join(", ", messages);
    }

    /**
     * 날씨 정보의 유효성 검사
     * @param weather 날씨 정보 객체
     * @return 날씨 정보가 유효한 경우 true, 그렇지 않은 경우 false
     */
    private boolean isWeatherAvailable(WeatherContext weather) {
        return weather != null && weather.getWalkLevel() != null && !"ERROR".equalsIgnoreCase(weather.getWalkLevel());
    }

    /**
     * 반려견의 나이 단계 추론
     * @param dog 반려견 정보 객체
     * @return 추론된 반려견 나이 단계
     */
    private DogStage inferDogStage(Dog dog) {
        if (dog == null) {
            return DogStage.UNKNOWN;
        }

        String source = normalizeText(String.join(" ",
                safeString(dog.getDogName()),
                safeString(dog.getDogBreed()),
                safeString(dog.getPersonality()),
                safeString(dog.getPreferredPlace())));

        if (containsAny(source, SENIOR_KEYWORDS)) {
            return DogStage.SENIOR;
        }
        if (containsAny(source, PUPPY_KEYWORDS)) {
            return DogStage.PUPPY;
        }
        return DogStage.ADULT;
    }

    /**
     * 반려견의 품종에 관련된 정보가 주어진 목록에 포함되어 있는지 확인
     * @param breed 확인할 품종 문자열
     * @param breeds 목록으로 확인할 품종 집합
     * @return 품종이 목록에 포함된 경우 true, 그렇지 않은 경우 false
     */
    private boolean containsBreed(String breed, Set<String> breeds) {
        return breeds.stream().map(this::normalizeText).anyMatch(breed::contains);
    }

    /**
     * 반려견이 열대에 취약한 품종인지 확인
     * @param dog 반려견 정보 객체
     * @return 열대에 취약한 품종인 경우 true, 그렇지 않은 경우 false
     */
    private boolean isHeatSensitive(Dog dog) {
        if (dog == null) {
            return false;
        }
        String breed = normalizeText(dog.getDogBreed());
        return containsBreed(breed, BRACHYCEPHALIC_BREEDS)
                || containsBreed(breed, LONG_HAIR_BREEDS)
                || dog.getDogSize() == Dog.DogSize.LARGE;
    }

    /**
     * 반려견이 추운 날씨에 취약한 품종인지 확인
     * @param dog 반려견 정보 객체
     * @return 추운 날씨에 취약한 품종인 경우 true, 그렇지 않은 경우 false
     */
    private boolean isColdSensitive(Dog dog) {
        if (dog == null) {
            return false;
        }
        return containsBreed(normalizeText(dog.getDogBreed()), SMALL_SENSITIVE_BREEDS)
                || dog.getDogSize() == Dog.DogSize.SMALL;
    }

    /**
     * 반려견이 긴 털을 가진 품종인지 확인
     * @param dog 반려견 정보 객체
     * @return 긴 털을 가진 품종인 경우 true, 그렇지 않은 경우 false
     */
    private boolean isLongHairBreed(Dog dog) {
        return dog != null && containsBreed(normalizeText(dog.getDogBreed()), LONG_HAIR_BREEDS);
    }

    /**
     * 텍스트에서 주어진 키워드 목록과 일치하는 단어의 개수를 세는 메서드
     * @param text 검사할 텍스트
     * @param keywords 일치 여부를 확인할 키워드 집합
     * @return 일치하는 키워드의 개수
     */
    private int countMatches(String text, Set<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (containsAny(text, Set.of(keyword))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 텍스트에서 주어진 키워드 목록과 일치하는 단어가 있는지 확인
     * @param text 검사할 텍스트
     * @param keywords 일치 여부를 확인할 키워드 집합
     * @return 일치하는 키워드가 있는 경우 true, 그렇지 않은 경우 false
     */
    private boolean containsAny(String text, Set<String> keywords) {
        String normalized = normalizeText(text);
        for (String keyword : keywords) {
            if (normalized.contains(normalizeText(keyword))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 여러 이유를 하나의 문자열로 합치는 메서드
     * @param reasons 합칠 이유 목록
     * @param fallback 합칠 이유가 없을 경우 사용할 기본 문자열
     * @return 합쳐진 이유 문자열
     */
    private String joinReasons(List<String> reasons, String fallback) {
        return reasons == null || reasons.isEmpty() ? fallback : String.join(" ", reasons);
    }

    /**
     * 텍스트를 정규화하는 메서드
     * @param value 정규화할 텍스트
     * @return 정규화된 텍스트
     */
    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace("[", " ")
                .replace("]", " ")
                .replace("\"", " ")
                .replace("'", " ")
                .replace("/", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }


    private String safeString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 값이 주어진 범위 내에 있는지 확인하는 메서드
     * @param value 확인할 값
     * @param min 최소값
     * @param max 최대값
     * @return 범위 내에 있는 경우 true, 그렇지 않은 경우 false
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * 주어진 값을 반올림하는 메서드
     * @param value 반올림할 값
     * @return 반올림된 값
     */
    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * 장소 프로필을 생성하는 메서드
     * @param place 장소 정보
     * @return 생성된 장소 프로필
     */
    private PlaceProfile buildPlaceProfile(Place place) {
        Set<String> tags = normalizeTags(place.getTags());
        String corpus = normalizeText(String.join(" ",
                safeString(place.getTitle()),
                safeString(place.getDescription()),
                safeString(place.getCategory()),
                safeString(place.getAddress()),
                safeString(place.getTags())));

        return new PlaceProfile(
                tags,
                corpus,
                place.getCategory(),
                place.getRating() == null ? 0.0 : place.getRating(),
                place.getReviewCount() == null ? 0 : place.getReviewCount(),
                place.getPhone() != null && !place.getPhone().isBlank(),
                place.getImageUrl() != null && !place.getImageUrl().isBlank(),
                place.getAddress() != null && !place.getAddress().isBlank()
        );
    }

    /**
     * 태그를 정규화하는 메서드
     * @param tags 정규화할 태그 문자열
     * @return 정규화된 태그 집합
     */
    private Set<String> normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(tags.split(","))
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 강아지의 나이 단계를 나타내는 열거형
     */
    private enum DogStage {
        PUPPY, ADULT, SENIOR, UNKNOWN
    }

    /**
     * 점수 구간을 나타내는 열거형
     */
    private enum ScoreSection {
        DOG("반려견 적합도", 35.0),
        WEATHER("날씨 적합도", 25.0),
        PLACE_ENV("장소 환경 적합도", 20.0),
        DISTANCE("거리/이동 편의성", 10.0),
        EXTRA("부가 요소", 10.0);

        private final String label;
        private final double maxScore;

        ScoreSection(String label, double maxScore) {
            this.label = label;
            this.maxScore = maxScore;
        }
    }

    /**
     *
     * @param tags
     * @param corpus
     * @param category
     * @param rating
     * @param reviewCount
     * @param phoneAvailable
     * @param imageAvailable
     * @param addressAvailable
     */
    private record PlaceProfile(
            Set<String> tags,
            String corpus,
            String category,
            double rating,
            int reviewCount,
            boolean phoneAvailable,
            boolean imageAvailable,
            boolean addressAvailable
    ) {
        private boolean hasKeyword(String keyword) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            return tags.stream().anyMatch(tag -> tag.contains(normalizedKeyword)) || corpus.contains(normalizedKeyword);
        }

        private boolean hasAny(Set<String> keywords) {
            for (String keyword : keywords) {
                if (hasKeyword(keyword)) {
                    return true;
                }
            }
            return false;
        }

        private boolean estimatedCrowdHigh() {
            return reviewCount >= 120 || (reviewCount >= 60 && "DINING".equalsIgnoreCase(category));
        }

        private boolean matchesPreference(String preferred) {
            return !preferred.isBlank() && corpus.contains(preferred);
        }
    }

    private final class ScoreAccumulator {
        private final Map<ScoreSection, Double> rawSectionScores = new EnumMap<>(ScoreSection.class);
        private final Map<ScoreSection, List<ScoreDetail>> detailMap = new EnumMap<>(ScoreSection.class);
        private final Set<ScoreSection> skippedSections = EnumSet.noneOf(ScoreSection.class);

        private void addDetail(ScoreSection section, String item, double maxScore, double score, String reason) {
            detailMap.computeIfAbsent(section, key -> new ArrayList<>())
                    .add(ScoreDetail.builder()
                            .section(section.label)
                            .item(item)
                            .score(round(score))
                            .maxScore(round(maxScore))
                            .reason(reason)
                            .build());

            rawSectionScores.merge(section, score, Double::sum);
        }

        private void skipSection(ScoreSection section, String reason) {
            skippedSections.add(section);
            detailMap.computeIfAbsent(section, key -> new ArrayList<>())
                    .add(ScoreDetail.builder()
                            .section(section.label)
                            .item("섹션 제외")
                            .score(0.0)
                            .maxScore(round(section.maxScore))
                            .reason(reason)
                            .build());
        }

        private FinalizedScore finalizeScores() {
            Map<String, Double> sectionScores = new LinkedHashMap<>();
            List<ScoreDetail> flattenedDetails = new ArrayList<>();
            double totalEarned = 0.0;
            double totalAvailable = 0.0;

            for (ScoreSection section : ScoreSection.values()) {
                flattenedDetails.addAll(detailMap.getOrDefault(section, List.of()));
                double sectionScore = skippedSections.contains(section)
                        ? 0.0
                        : clamp(rawSectionScores.getOrDefault(section, 0.0), 0.0, section.maxScore);

                sectionScores.put(section.label, round(sectionScore));

                if (!skippedSections.contains(section)) {
                    totalEarned += sectionScore;
                    totalAvailable += section.maxScore;
                }
            }

            double normalizedTotal = totalAvailable == 0.0 ? 0.0 : round((totalEarned / totalAvailable) * 100.0);
            return new FinalizedScore(sectionScores, flattenedDetails, normalizedTotal);
        }
    }

    private record FinalizedScore(
            Map<String, Double> sectionScores,
            List<ScoreDetail> details,
            double totalScore
    ) {
        private double sectionScore(ScoreSection section) {
            return sectionScores.getOrDefault(section.label, 0.0);
        }
    }
}
