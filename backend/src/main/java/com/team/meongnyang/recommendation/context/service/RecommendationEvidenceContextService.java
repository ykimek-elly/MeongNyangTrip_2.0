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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

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

            [상위 장소 근거]
            %s

            [추가 지침]
            %s
            """.formatted(
                    userProfileSection,
                    petProfileSection,
                    weatherSection,
                    recommendationDecisionSummary,
                    topPlaceEvidenceSection,
                    supplementalGuidelineSection
            ),
            MAX_SNAPSHOT_LENGTH
    );

    log.info("[추천 컨텍스트] 생성 완료 snapshotLength={}, rankedCount={}",
            contextSnapshot.length(),
            rankedPlaces == null ? 0 : rankedPlaces.size());

    return RecommendationEvidenceContext.builder()
            .userProfileSection(userProfileSection)
            .petProfileSection(petProfileSection)
            .weatherSection(weatherSection)
            .recommendationDecisionSummary(recommendationDecisionSummary)
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
            - 알림 활성화: %s
            """.formatted(
            RecommendationTextUtils.defaultIfBlank(user.getNickname(), "알 수 없음"),
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
            RecommendationTextUtils.defaultIfBlank(pet.getPetName(), "알 수 없음"),
            RecommendationTextUtils.defaultIfBlank(pet.getPetBreed(), "알 수 없음"),
            pet.getPetSize() == null ? "알 수 없음" : pet.getPetSize().name(),
            pet.getPetAge() == null ? "알 수 없음" : pet.getPetAge(),
            pet.getPetActivity() == null ? "알 수 없음" : pet.getPetActivity().name(),
            RecommendationTextUtils.defaultIfBlank(pet.getPersonality(), "알 수 없음"),
            RecommendationTextUtils.defaultIfBlank(pet.getPreferredPlace(), "알 수 없음")
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
            - 산책 가능 레벨: %s
            - 기온: %.1f도
            - 습도: %d%%
            - 강수 형태: %s
            - 강수량: %.1fmm
            - 풍속: %.1fm/s
            - 주요 제약: %s
            """.formatted(
            RecommendationTextUtils.defaultIfBlank(weather.getWalkLevel(), "알 수 없음"),
            weather.getTemperature(),
            weather.getHumidity(),
            RecommendationTextUtils.defaultIfBlank(weather.getPrecipitationType(), "알 수 없음"),
            weather.getRainfall(),
            weather.getWindSpeed(),
            constraintSummary
    ).trim();
  }

  private String buildRecommendationDecisionSummary(WeatherContext weather, List<ScoredPlace> rankedPlaces) {
    String weatherPriority = describeWeatherDecision(weather);
    String scorePriority = rankedPlaces == null || rankedPlaces.isEmpty()
            ? "순위 정보 없음"
            : topScoreFocus(rankedPlaces.get(0));

    return """
            - 추천 순위는 서버에서 이미 계산됨
            - 날씨 우선 판단: %s
            - 상위 점수 축: %s
            - 1순위가 왜 가장 적합한지와 2, 3순위의 차이를 설명해야 함
            """.formatted(weatherPriority, scorePriority).trim();
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

      sb.append(rank).append("위 ").append(RecommendationTextUtils.defaultIfBlank(place.getTitle(), "장소 정보 없음")).append("\n")
              .append("   - 카테고리: ").append(RecommendationTextUtils.defaultIfBlank(place.getCategory(), "알 수 없음")).append("\n")
              .append("   - 총점: ").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getTotalScore())).append("\n")
              .append("   - 점수 구성: 반려동물=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getDogFitScore()))
              .append(", 날씨=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getWeatherScore()))
              .append(", 환경=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getPlaceEnvScore()))
              .append(", 거리=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getDistanceScore()))
              .append(", 가산=").append(RecommendationNumberUtils.roundOneDecimal(scoredPlace.getHistoryScore()))
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
    return "- 추가 지침 없음";
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
      parts.add("한줄 소개=" + RecommendationTextUtils.abbreviate(
              RecommendationTextUtils.singleLine(place.getOverview()),
              MAX_OVERVIEW_LENGTH
      ));
    }
    return String.join(", ", parts);
  }

  private String formatPositiveDetail(ScoreDetail detail) {
    String section = RecommendationTextUtils.defaultIfBlank(detail.getSection(), "항목");
    String item = RecommendationTextUtils.defaultIfBlank(detail.getItem(), "세부값");
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
            + RecommendationTextUtils.defaultIfBlank(detail.getItem(), "세부값")
            + " 확인 필요";
  }

  private String describeWeatherDecision(WeatherContext weather) {
    if (weather == null) {
      return "날씨 정보 없음";
    }
    if (weather.isRaining() || weather.isWindy()) {
      return "날씨 노출이 적은 실내 또는 혼합형 장소를 우선 고려";
    }
    if (weather.isHot()) {
      return "더위 부담과 이동 피로가 적은 장소를 우선 고려";
    }
    if (weather.isCold()) {
      return "실내 쾌적성과 짧은 외출 동선을 우선 고려";
    }
    return "날씨 부담이 적어 반려동물 적합도와 장소 품질이 순위를 주도";
  }

  private String topScoreFocus(ScoredPlace topPlace) {
    List<String> focus = new ArrayList<>();
    focus.add(scoreFocusLabel("반려동물 적합도", topPlace.getDogFitScore()));
    focus.add(scoreFocusLabel("날씨 적합도", topPlace.getWeatherScore()));
    focus.add(scoreFocusLabel("환경 점수", topPlace.getPlaceEnvScore()));
    focus.add(scoreFocusLabel("거리 편의", topPlace.getDistanceScore()));

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
