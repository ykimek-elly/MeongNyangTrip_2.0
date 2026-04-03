package com.team.meongnyang.recommendation.context.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.dto.ScoreBreakdown;
import com.team.meongnyang.recommendation.dto.ScoreDetail;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.util.RecommendationNumberUtils;
import com.team.meongnyang.recommendation.util.RecommendationTextUtils;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 결과 설명 생성에 필요한 근거 컨텍스트를 구성한다.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class RecommendationEvidenceContextService {

  private static final int MAX_PLACE_COUNT = 3;
  private static final int MAX_STRENGTH_COUNT = 3;
  private static final int MAX_CAUTION_COUNT = 2;
  private static final int MAX_OVERVIEW_LENGTH = 100;
  private static final int MAX_SNAPSHOT_LENGTH = 1200;

  public RecommendationEvidenceContext buildContext(
          User user,
          Pet pet,
          WeatherContext weather,
          List<ScoredPlace> rankedPlaces
  ) {
    String userProfileSection = buildUserProfileSection(user);
    String petProfileSection = buildPetProfileSection(pet);
    String weatherSection = buildWeatherSection(weather);
    String recommendationDecisionSummary = buildRecommendationDecisionSummary(weather, rankedPlaces);
    String explanationFocusSection = buildExplanationFocusSection(pet, weather, rankedPlaces);
    String topPlaceEvidenceSection = buildTopPlaceEvidenceSection(rankedPlaces);
    String supplementalGuidelineSection = buildSupplementalGuidelineSection();

    String contextSnapshot = RecommendationTextUtils.abbreviate(
            """
            [사용자 정보]
            %s

            [반려동물 정보]
            %s

            [날씨 정보]
            %s

            [추천 판단 요약]
            %s

            [설명 필수 근거]
            %s

            [상위 장소 근거]
            %s

            [추가 지침]
            %s
            """.formatted(
                    userProfileSection,
                    petProfileSection,
                    weatherSection,
                    recommendationDecisionSummary,
                    explanationFocusSection,
                    topPlaceEvidenceSection,
                    supplementalGuidelineSection
            ),
            MAX_SNAPSHOT_LENGTH
    );

    return RecommendationEvidenceContext.builder()
            .userProfileSection(userProfileSection)
            .petProfileSection(petProfileSection)
            .weatherSection(weatherSection)
            .recommendationDecisionSummary(recommendationDecisionSummary)
            .explanationFocusSection(explanationFocusSection)
            .topPlaceEvidenceSection(topPlaceEvidenceSection)
            .supplementalGuidelineSection(supplementalGuidelineSection)
            .contextSnapshot(contextSnapshot)
            .build();
  }

  private String buildUserProfileSection(User user) {
    if (user == null) {
      return "- 사용자 정보 없음";
    }

    return """
            - 닉네임: %s
            - 알림 수신: %s
            """.formatted(
            RecommendationTextUtils.defaultIfBlank(user.getNickname(), "정보 없음"),
            user.isNotificationEnabled() ? "예" : "아니오"
    ).trim();
  }

  private String buildPetProfileSection(Pet pet) {
    if (pet == null) {
      return "- 반려동물 정보 없음";
    }

    return """
            - 이름: %s
            - 품종: %s
            - 크기: %s
            - 나이: %s
            - 활동량: %s
            - 성향: %s
            - 선호 장소: %s
            """.formatted(
            RecommendationTextUtils.defaultIfBlank(pet.getPetName(), "정보 없음"),
            RecommendationTextUtils.defaultIfBlank(pet.getPetBreed(), "정보 없음"),
            pet.getPetSize() == null ? "정보 없음" : pet.getPetSize().name(),
            pet.getPetAge() == null ? "정보 없음" : pet.getPetAge(),
            pet.getPetActivity() == null ? "정보 없음" : pet.getPetActivity().name(),
            RecommendationTextUtils.defaultIfBlank(pet.getPersonality(), "정보 없음"),
            RecommendationTextUtils.defaultIfBlank(pet.getPreferredPlace(), "정보 없음")
    ).trim();
  }

  private String buildWeatherSection(WeatherContext weather) {
    if (weather == null) {
      return "- 날씨 정보 없음";
    }

    List<String> constraints = new ArrayList<>();
    if (weather.isRaining()) {
      constraints.add("비");
    }
    if (weather.isWindy()) {
      constraints.add("강풍");
    }
    if (weather.isHot()) {
      constraints.add("더위");
    }
    if (weather.isCold()) {
      constraints.add("추위");
    }

    String constraintSummary = constraints.isEmpty()
            ? "주요 제약 없음"
            : String.join(", ", constraints);

    return """
            - 산책 가능 등급: %s
            - 기온: %.1f도
            - 습도: %d%%
            - 강수 형태: %s
            - 강수량: %.1fmm
            - 풍속: %.1fm/s
            - 주요 제약: %s
            """.formatted(
            RecommendationTextUtils.defaultIfBlank(weather.getWalkLevel(), "정보 없음"),
            weather.getTemperature(),
            weather.getHumidity(),
            RecommendationTextUtils.defaultIfBlank(weather.getPrecipitationType(), "정보 없음"),
            weather.getRainfall(),
            weather.getWindSpeed(),
            constraintSummary
    ).trim();
  }

  private String buildRecommendationDecisionSummary(WeatherContext weather, List<ScoredPlace> rankedPlaces) {
    String weatherPriority = describeWeatherDecision(weather);
    String scorePriority = rankedPlaces == null || rankedPlaces.isEmpty()
            ? "상위 점수 정보 없음"
            : topScoreFocus(rankedPlaces.get(0));

    return """
            - 추천 순위는 서비스에서 이미 계산됨
            - 날씨 우선 판단: %s
            - 상위 점수 축: %s
            - 1위가 왜 맞는지와 2, 3위와의 차이를 설명해야 함
            """.formatted(weatherPriority, scorePriority).trim();
  }

  private String buildExplanationFocusSection(Pet pet, WeatherContext weather, List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "- 설명용 근거 없음";
    }

    ScoredPlace topPlace = rankedPlaces.get(0);
    List<ScoreDetail> details = topPlace.getScoreDetails() == null ? List.of() : topPlace.getScoreDetails();
    List<ScoreDetail> positiveDetails = details.stream()
            .filter(detail -> detail.getScore() > 0.0)
            .filter(detail -> detail.getScore() >= Math.max(1.0, detail.getMaxScore() * 0.35))
            .sorted(Comparator
                    .comparingDouble((ScoreDetail detail) -> detail.getScore() / Math.max(detail.getMaxScore(), 1.0))
                    .reversed()
                    .thenComparing(ScoreDetail::getScore, Comparator.reverseOrder()))
            .toList();

    List<String> weatherEvidence = positiveDetails.stream()
            .filter(this::isWeatherDetail)
            .map(detail -> toBoostNarrative(detail, topPlace.getPlace(), pet, weather))
            .filter(text -> !text.isBlank())
            .distinct()
            .toList();

    List<String> petEvidence = positiveDetails.stream()
            .filter(this::isPetDetail)
            .map(detail -> toBoostNarrative(detail, topPlace.getPlace(), pet, weather))
            .filter(text -> !text.isBlank())
            .distinct()
            .toList();

    List<String> generalEvidence = positiveDetails.stream()
            .filter(detail -> !isWeatherDetail(detail) && !isPetDetail(detail))
            .map(detail -> toBoostNarrative(detail, topPlace.getPlace(), pet, weather))
            .filter(text -> !text.isBlank())
            .distinct()
            .toList();

    Set<String> selectedEvidence = new LinkedHashSet<>();
    if (!weatherEvidence.isEmpty()) {
      selectedEvidence.add(weatherEvidence.get(0));
    }
    if (!petEvidence.isEmpty()) {
      selectedEvidence.add(petEvidence.get(0));
    }
    generalEvidence.forEach(selectedEvidence::add);
    weatherEvidence.stream().skip(1).forEach(selectedEvidence::add);
    petEvidence.stream().skip(1).forEach(selectedEvidence::add);

    if (selectedEvidence.size() < 2) {
      extractTopStrengths(topPlace).stream()
              .map(this::normalizeFallbackStrength)
              .filter(text -> !text.isBlank())
              .forEach(selectedEvidence::add);
    }

    List<String> mandatoryEvidence = selectedEvidence.stream()
            .limit(Math.max(2, Math.min(4, selectedEvidence.size())))
            .toList();

    List<String> cautionNarratives = topPlace.getAppliedPenalties() == null
            ? List.of()
            : topPlace.getAppliedPenalties().stream()
            .map(this::toPenaltyNarrative)
            .filter(text -> !text.isBlank())
            .distinct()
            .limit(2)
            .toList();

    List<String> lines = new ArrayList<>();
    lines.add("- 1위 장소: " + RecommendationTextUtils.defaultIfBlank(topPlace.getPlace().getTitle(), "장소 정보 없음"));
    mandatoryEvidence.forEach(evidence -> lines.add("- 반드시 설명에 포함할 근거: " + evidence));
    if (!cautionNarratives.isEmpty()) {
      cautionNarratives.forEach(caution -> lines.add("- 참고할 감점 맥락: " + caution));
    }

    String comparisonHint = buildComparisonHint(rankedPlaces);
    if (!comparisonHint.isBlank()) {
      lines.add("- 비교 포인트: " + comparisonHint);
    }

    return String.join("\n", lines);
  }

  private String buildTopPlaceEvidenceSection(List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "- 정렬된 추천 장소 없음";
    }

    StringBuilder sb = new StringBuilder();
    int rank = 1;

    for (ScoredPlace scoredPlace : rankedPlaces.stream().limit(MAX_PLACE_COUNT).toList()) {
      Place place = scoredPlace.getPlace();
      List<String> strengths = extractTopStrengths(scoredPlace);
      List<String> cautions = extractCautions(scoredPlace);

      sb.append(rank).append("위. ").append(RecommendationTextUtils.defaultIfBlank(place.getTitle(), "장소 정보 없음")).append("\n")
              .append("   - 카테고리: ").append(RecommendationTextUtils.defaultIfBlank(place.getCategory(), "정보 없음")).append("\n")
              .append("   - 총점: ").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getTotalScore())).append("\n")
              .append("   - 점수 구성: 개인=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getPersonalFitScore()))
              .append(", 날씨=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getWeatherFitScore()))
              .append(", 환경=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getEnvironmentFitScore()))
              .append(", 이동=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getMobilityFitScore()))
              .append(", 보너스=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getBonusScore()))
              .append(", 감점=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getPenaltyScore())).append("\n")
              .append("   - 메타데이터: ").append(buildMetadataLine(place)).append("\n")
              .append("   - 강점: ").append(joinOrFallback(strengths, "없음")).append("\n")
              .append("   - 주의점: ").append(joinOrFallback(cautions, "없음")).append("\n")
              .append("   - 점수 요약: ").append(RecommendationTextUtils.defaultIfBlank(scoredPlace.getSummary(), "없음")).append("\n")
              .append("   - 추천 이유: ").append(RecommendationTextUtils.defaultIfBlank(scoredPlace.getReason(), "없음")).append("\n");

      rank++;
      if (rank <= MAX_PLACE_COUNT && rank - 1 < rankedPlaces.size()) {
        sb.append("\n");
      }
    }

    return sb.toString().trim();
  }

  private String buildSupplementalGuidelineSection() {
    return """
            - 설명은 1위 장소의 필수 근거를 먼저 사용
            - boost 항목은 점수명이 아니라 실제 이용 상황 문장으로 풀어 쓸 것
            - '좋습니다', '추천드립니다', '적합합니다' 같은 단순 권유 표현은 사용하지 말 것
            """.trim();
  }

  private List<String> extractTopStrengths(ScoredPlace scoredPlace) {
    List<String> strengths = new ArrayList<>();

    if (scoredPlace.getBreakdowns() != null) {
      strengths.addAll(
              scoredPlace.getBreakdowns().stream()
                      .sorted(Comparator.comparingDouble(ScoreBreakdown::getScore).reversed())
                      .map(ScoreBreakdown::getSummary)
                      .filter(Objects::nonNull)
                      .map(RecommendationTextUtils::singleLine)
                      .filter(text -> !text.isBlank())
                      .limit(MAX_STRENGTH_COUNT)
                      .toList()
      );
    }

    if (strengths.size() < MAX_STRENGTH_COUNT && scoredPlace.getScoreDetails() != null) {
      strengths.addAll(
              scoredPlace.getScoreDetails().stream()
                      .filter(detail -> detail.getScore() > 0.0)
                      .sorted(Comparator.comparingDouble(ScoreDetail::getScore).reversed())
                      .map(this::formatPositiveDetail)
                      .filter(text -> !text.isBlank())
                      .toList()
      );
    }

    return strengths.stream()
            .map(RecommendationTextUtils::singleLine)
            .filter(text -> !text.isBlank())
            .distinct()
            .limit(MAX_STRENGTH_COUNT)
            .toList();
  }

  private List<String> extractCautions(ScoredPlace scoredPlace) {
    List<String> cautions = new ArrayList<>();

    if (scoredPlace.getPenaltyScore() > 0.0) {
      cautions.add("감점 적용: " + RecommendationNumberUtils.roundOneDecimal(scoredPlace.getPenaltyScore()));
    }

    if (scoredPlace.getScoreDetails() != null) {
      cautions.addAll(
              scoredPlace.getScoreDetails().stream()
                      .filter(detail -> detail.getScore() <= 0.0 || containsRisk(detail.getReason()))
                      .map(this::formatCautionDetail)
                      .filter(text -> !text.isBlank())
                      .toList()
      );
    }

    return cautions.stream()
            .map(RecommendationTextUtils::singleLine)
            .filter(text -> !text.isBlank())
            .distinct()
            .limit(MAX_CAUTION_COUNT)
            .toList();
  }

  private String buildMetadataLine(Place place) {
    List<String> parts = new ArrayList<>();
    parts.add("검증=" + (Boolean.TRUE.equals(place.getIsVerified()) ? "예" : "아니오"));
    parts.add("평점=" + RecommendationNumberUtils.roundOneDecimal(place.getRating()));
    parts.add("리뷰 수=" + (place.getReviewCount() == null ? 0 : place.getReviewCount()));

    if (place.getAiRating() != null) {
      parts.add("AI 평점=" + RecommendationNumberUtils.roundOneDecimal(place.getAiRating()));
    }
    if (place.getChkPetInside() != null && !place.getChkPetInside().isBlank()) {
      parts.add("실내 동반=" + place.getChkPetInside());
    }
    if (place.getTags() != null && !place.getTags().isBlank()) {
      parts.add("태그=" + RecommendationTextUtils.abbreviate(place.getTags(), 80));
    }
    if (place.getOverview() != null && !place.getOverview().isBlank()) {
      parts.add("설명=" + RecommendationTextUtils.abbreviate(
              RecommendationTextUtils.singleLine(place.getOverview()),
              MAX_OVERVIEW_LENGTH
      ));
    }
    return String.join(", ", parts);
  }

  private String formatPositiveDetail(ScoreDetail detail) {
    String section = RecommendationTextUtils.defaultIfBlank(detail.getSection(), "항목");
    String item = RecommendationTextUtils.defaultIfBlank(detail.getItem(), "세부 항목");
    String reason = RecommendationTextUtils.defaultIfBlank(detail.getReason(), "");

    return reason.isBlank()
            ? section + "/" + item + " +" + RecommendationNumberUtils.roundOneDecimal(detail.getScore())
            : section + "/" + item + " +" + RecommendationNumberUtils.roundOneDecimal(detail.getScore()) + " (" + reason + ")";
  }

  private String formatCautionDetail(ScoreDetail detail) {
    String reason = RecommendationTextUtils.defaultIfBlank(detail.getReason(), "");
    if (!reason.isBlank()) {
      return reason;
    }

    return RecommendationTextUtils.defaultIfBlank(detail.getSection(), "항목")
            + "/"
            + RecommendationTextUtils.defaultIfBlank(detail.getItem(), "세부 항목")
            + " 확인 필요";
  }

  private boolean isWeatherDetail(ScoreDetail detail) {
    String section = RecommendationTextUtils.defaultIfBlank(detail.getSection(), "");
    return section.contains("날씨");
  }

  private boolean isPetDetail(ScoreDetail detail) {
    String section = RecommendationTextUtils.defaultIfBlank(detail.getSection(), "");
    return section.contains("반려동물") || section.contains("펫");
  }

  private String toBoostNarrative(ScoreDetail detail, Place place, Pet pet, WeatherContext weather) {
    String item = RecommendationTextUtils.defaultIfBlank(detail.getItem(), "");
    String placeTitle = place == null ? "이 장소" : RecommendationTextUtils.defaultIfBlank(place.getTitle(), "이 장소");

    return switch (item) {
      case "산책등급" -> weather != null && (weather.isRaining() || weather.isWindy() || weather.isCold() || weather.isHot())
              ? "현재 날씨 변수 안에서도 이동 동선을 조절하기 쉬워 외출 부담을 덜어줌"
              : "산책하기 편한 흐름이라 야외 공간과 동선을 활용하기 쉬움";
      case "강수" -> weather != null && weather.isRaining()
              ? "비나 눈 영향을 덜 받는 구조라 이동 동선이 비교적 안정적임"
              : "강수 부담이 거의 없어 야외 체류 시간을 확보하기 쉬움";
      case "기온" -> weather != null && weather.isHot()
              ? "더위를 피할 여지가 있어 체온 부담을 줄이기 쉬움"
              : weather != null && weather.isCold()
              ? "추위를 피할 여지가 있어 체감 온도 부담을 낮추기 쉬움"
              : "기온 부담이 크지 않아 머무는 흐름이 한결 안정적임";
      case "바람" -> weather != null && weather.isWindy()
              ? "바람을 직접 덜 맞는 구조라 야외 활동 피로가 덜함"
              : "바람 영향이 크지 않아 야외 활동 동선이 한결 편함";
      case "품종" -> pet != null
              ? RecommendationTextUtils.defaultIfBlank(pet.getPetBreed(), "반려동물") + "의 일반적인 성향과 장소 유형이 무리 없이 맞물림"
              : "반려동물 특성과 장소 유형이 크게 어긋나지 않음";
      case "성향" -> "반려동물 성향과 장소 분위기가 맞물려 자극이 과하지 않음";
      case "활동량" -> "반려동물 활동량에 맞는 동선과 체류 리듬을 만들기 쉬움";
      case "크기" -> "반려동물 체형을 고려했을 때 공간 밀도와 이동 여유가 무난한 편임";
      case "나이" -> "반려동물 나이대에 맞춰 활동 강도나 휴식 동선을 조절하기 쉬움";
      case "대표견 선호 장소" -> "평소 선호하는 장소 유형과 결이 맞아 낯선 환경 부담을 줄이기 쉬움";
      case "동반 친화성", "실내 동반 가능성" -> placeTitle + "은 반려동물과 함께 들어가고 머무를 동선 판단이 비교적 명확함";
      case "안전/쾌적성" -> "혼잡이나 자극 요소를 덜 받는 편이라 머무는 동안 안정감을 기대할 수 있음";
      case "기본 시설", "이용 편의" -> "반려동물과 함께 이용할 때 필요한 기본 편의 요소가 있어 이동이 덜 번거로움";
      case "정책 명확성", "제약 수준" -> "동반 정책이 비교적 분명해 현장 혼선 가능성이 낮음";
      case "거리", "좌표정보" -> "이동 거리가 과하지 않아 짧은 외출 일정으로 묶기 쉬움";
      case "검증 여부" -> "검증 이력이 있어 기본 정보 신뢰도가 상대적으로 안정적임";
      case "설명 품질", "태그 품질", "데이터 완성도" -> "장소 정보가 구체적이라 방문 전 판단 근거를 확보하기 쉬움";
      case "품질 지표", "공개 평점", "리뷰 표본 신뢰도", "AI 보조 평점", "블로그 표본 신뢰도" -> "평점과 리뷰 표본이 받쳐줘 실제 이용 품질을 가늠하기 쉬움";
      case "긍정 태그" -> "실제 방문 후기에 긍정 신호가 누적돼 체감 만족도를 기대할 근거가 있음";
      case "태그 적합도", "카테고리 적합성", "카테고리 기본점" -> "장소 카테고리와 핵심 태그가 현재 외출 목적과 잘 맞물림";
      default -> {
        String reason = RecommendationTextUtils.defaultIfBlank(detail.getReason(), "");
        if (!reason.isBlank()) {
          yield RecommendationTextUtils.singleLine(reason);
        }
        yield RecommendationTextUtils.defaultIfBlank(detail.getSection(), "항목")
                + "의 "
                + RecommendationTextUtils.defaultIfBlank(item, "세부 항목")
                + " 점수가 높게 반영됨";
      }
    };
  }

  private String toPenaltyNarrative(String penalty) {
    if (penalty == null || penalty.isBlank()) {
      return "";
    }

    String normalized = penalty.toLowerCase(Locale.ROOT);
    if (normalized.contains("선호 장소")) {
      return "선호 장소와 완전히 같지는 않지만 다른 강점이 이를 상쇄함";
    }
    if (normalized.contains("비")) {
      return "강수 상황에서는 체류 방식에 약간의 제약이 생길 수 있음";
    }
    if (normalized.contains("강풍")) {
      return "바람이 강해지면 야외 체류 피로가 늘 수 있음";
    }
    if (normalized.contains("중복")) {
      return "최근 추천 이력 때문에 우선순위가 일부 조정되었음";
    }
    return RecommendationTextUtils.singleLine(penalty);
  }

  private String buildComparisonHint(List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.size() <= 1) {
      return "";
    }

    List<String> comparisons = new ArrayList<>();
    for (int i = 1; i < Math.min(rankedPlaces.size(), MAX_PLACE_COUNT); i++) {
      ScoredPlace candidate = rankedPlaces.get(i);
      String name = candidate.getPlace() == null
              ? "후보 장소"
              : RecommendationTextUtils.defaultIfBlank(candidate.getPlace().getTitle(), "후보 장소");
      List<String> cautions = extractCautions(candidate);
      String clue = cautions.isEmpty()
              ? RecommendationTextUtils.defaultIfBlank(candidate.getSummary(), "근거 부족")
              : cautions.get(0);
      comparisons.add(name + "은 " + RecommendationTextUtils.singleLine(clue));
    }
    return String.join(" / ", comparisons);
  }

  private String normalizeFallbackStrength(String strength) {
    if (strength == null || strength.isBlank()) {
      return "";
    }
    if (strength.contains("+")) {
      return strength.replaceAll("\\s*\\+\\d+(\\.\\d+)?", "").trim() + " 점수가 높게 반영됨";
    }
    return RecommendationTextUtils.singleLine(strength);
  }

  private String describeWeatherDecision(WeatherContext weather) {
    if (weather == null) {
      return "날씨 정보 없음";
    }
    if (weather.isRaining() || weather.isWindy()) {
      return "날씨 변수에 덜 흔들리는 실내 또는 보호된 동선이 있는 장소를 우선 고려";
    }
    if (weather.isHot()) {
      return "더위를 피하거나 이동 피로가 덜한 장소를 우선 고려";
    }
    if (weather.isCold()) {
      return "실내 체류와 짧은 이동 동선이 가능한 장소를 우선 고려";
    }
    return "날씨 부담이 적어 반려동물 궁합이 좋은 장소가 순위를 주도";
  }

  private String topScoreFocus(ScoredPlace topPlace) {
    List<String> focus = new ArrayList<>();
    focus.add(scoreFocusLabel("개인화 적합도", topPlace.getPersonalFitScore()));
    focus.add(scoreFocusLabel("날씨 적합도", topPlace.getWeatherFitScore()));
    focus.add(scoreFocusLabel("환경 점수", topPlace.getEnvironmentFitScore()));
    focus.add(scoreFocusLabel("이동 편의성", topPlace.getMobilityFitScore()));

    return focus.stream()
            .sorted(Comparator.comparingDouble(this::parseScore).reversed())
            .map(value -> value.substring(0, value.indexOf('=')))
            .limit(2)
            .collect(Collectors.joining(", "));
  }

  private String scoreFocusLabel(String label, double score) {
    return label + "=" + RecommendationNumberUtils.roundOneDecimal(score);
  }

  private double parseScore(String labeledScore) {
    try {
      return Double.parseDouble(labeledScore.substring(labeledScore.indexOf('=') + 1));
    } catch (Exception e) {
      return 0.0;
    }
  }

  private boolean containsRisk(String reason) {
    if (reason == null || reason.isBlank()) {
      return false;
    }

    String normalized = reason.toLowerCase(Locale.ROOT);
    return normalized.contains("주의")
            || normalized.contains("불리")
            || normalized.contains("감점")
            || normalized.contains("위험")
            || normalized.contains("risk")
            || normalized.contains("penalty")
            || normalized.contains("caution");
  }

  private String joinOrFallback(List<String> values, String fallback) {
    if (values == null || values.isEmpty()) {
      return fallback;
    }
    return String.join(" | ", values);
  }
}
