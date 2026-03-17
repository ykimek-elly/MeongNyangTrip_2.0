package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천 오케스트레이션에서 수집한 데이터를 Gemini 입력 프롬프트로 조합하는 서비스이다.
 *
 * <p>점수 계산이 끝난 후보 장소, 날씨 문맥, 사용자와 반려동물 정보, RAG 문맥을
 * 모델이 바로 사용할 수 있는 단일 문자열로 정리한다. 이 프롬프트는 이후 Gemini 호출과
 * 캐시 키 생성의 기준이 되며, 최종 추천 문장의 품질을 좌우한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationPromptService {

  /**
   * 추천에 필요한 핵심 입력을 하나의 Gemini 프롬프트 문자열로 구성한다.
   *
   * @param user 추천 대상 사용자 정보
   * @param pet 추천 기준이 되는 반려동물 정보
   * @param weather 추천 시점의 날씨 문맥
   * @param rankedPlaces 점수 계산 후 정렬된 상위 후보 장소 목록
   * @param ragContext 추천 설명에 반영할 RAG 문맥
   * @return Gemini 호출과 캐시 키 생성에 사용할 최종 프롬프트 문자열
   */
  public String buildRecommendationPrompt(
          User user,
          Pet pet,
          WeatherContext weather,
          List<ScoredPlace> rankedPlaces,
          String ragContext
  ) {
    // 1. 사용자, 반려견, 날씨의 원본 데이터를 프롬프트용 값으로 정리한다.
    String nickname = user != null ? nullSafe(user.getNickname()) : "정보 없음";
    String petName = pet != null ? nullSafe(pet.getPetName()) : "정보 없음";
    String petBreed = pet != null ? nullSafe(pet.getPetBreed()) : "정보 없음";
    String petSize = pet != null && pet.getPetSize() != null ? String.valueOf(pet.getPetSize()) : "정보 없음";
    String petPersonality = pet != null ? nullSafe(pet.getPersonality()) : "정보 없음";
    String preferredPlace = safePreferredPlace(pet);

    double temperature = weather != null ? weather.getTemperature() : 0.0;
    int humidity = weather != null ? weather.getHumidity() : 0;
    String precipitationType = weather != null ? nullSafe(weather.getPrecipitationType()) : "정보 없음";
    double rainfall = weather != null ? weather.getRainfall() : 0.0;
    double windSpeed = weather != null ? weather.getWindSpeed() : 0.0;
    boolean raining = weather != null && weather.isRaining();
    boolean cold = weather != null && weather.isCold();
    boolean hot = weather != null && weather.isHot();
    boolean windy = weather != null && weather.isWindy();
    String walkLevel = weather != null ? nullSafe(weather.getWalkLevel()) : "정보 없음";

    // 2. 모델이 바로 활용할 수 있도록 요약 블록을 조립한다.
    String placeSection = buildPlaceSection(rankedPlaces);
    String weatherSummary = buildWeatherSummary(weather);
    String petSummary = buildPetSummary(pet);
    String ragSection = buildRagSection(ragContext);

    // 3. 역할, 입력 데이터, 출력 지시를 분리한 최종 프롬프트 문자열을 만든다.
    String prompt = """
            [SYSTEM ROLE]
            당신은 반려견과 함께 외출할 장소를 추천하는 실사용 추천 어시스턴트다.
            단순 정보 요약이 아니라, 지금 사용자 상황에서 가장 적절한 선택지를 자연스럽게 추천해야 한다.

            [GOAL]
            아래 입력 데이터를 바탕으로 사용자에게 보내는 최종 추천 문장을 작성하라.
            가장 중요한 목표는 "왜 1순위가 지금 가장 적합한지"를 점수 항목과 현재 상황에 연결해 설득력 있게 설명하는 것이다.

            [WRITING RULES]
            - 말투는 안내문이 아니라 실제 사용자에게 말하듯 자연스럽고 부드럽게 작성한다.
            - 입력에 있는 정보는 자연스럽게 연결하되, 항목을 기계적으로 나열하지 않는다.
            - 1순위 장소는 반드시 반려동물 적합도, 날씨 적합도, 거리/이동 편의 중 2개 이상을 근거로 설명한다.
            - 상위 3개 장소가 서로 왜 다른지 분명하게 드러나야 한다.
            - 2순위와 3순위는 각각 어떤 상황에서 더 어울리는 선택인지 구분해서 설명한다.
            - RAG 참고 문서는 일반 상식이나 통계 설명용이 아니라, 현재 추천에 직접 필요한 주의점이나 맥락만 짧게 반영한다.
            - RAG 내용이 추천과 직접 관련 없으면 억지로 사용하지 않는다.
            - 없는 정보는 추측하지 말고 입력값만 사용한다.
            - 점수는 설명 근거로만 사용하고 숫자를 과도하게 반복하지 않는다.
            - 첫 문장은 현재 날씨가 오늘 추천 방향에 어떤 영향을 주는지 먼저 설명한다.
            - Top3 장소는 모두 최소 1회 이상 언급하되, 1순위는 가장 자세히 설명한다.

            [OUTPUT INSTRUCTIONS]
            - 한국어로 작성한다.
            - 5~7문장 정도의 짧은 추천 메시지로 작성한다.
            - 첫 문장에는 오늘 날씨와 반려견 특성을 함께 반영한 전체 판단을 넣는다.
            - 다음 2~3문장에서는 1순위 장소를 가장 자세히 설명한다.
            - 마지막 2문장 안에서는 2순위와 3순위를 비교하며 각각 어떤 상황에서 더 어울리는지 구분한다.
            - 문장 안에서 "1순위", "2순위" 같은 표현은 써도 되지만, 보고서처럼 딱딱하게 쓰지 않는다.

            [INPUT DATA]
            사용자:
            - 닉네임: %s

            반려견:
            - 이름: %s
            - 품종: %s
            - 크기: %s
            - 성향: %s
            - 선호 장소: %s
            - 요약: %s

            현재 날씨:
            - 기온: %.1f도
            - 습도: %d%%
            - 강수 형태: %s
            - 강수량: %.1fmm
            - 풍속: %.1fm/s
            - 비 여부: %s
            - 추위 여부: %s
            - 더위 여부: %s
            - 강풍 여부: %s
            - 산책 가능 레벨: %s
            - 해석: %s

            추천 후보 Top 3:
            %s

            RAG 참고 맥락:
            %s
            """
            .formatted(
                    nickname,
                    petName,
                    petBreed,
                    petSize,
                    petPersonality,
                    preferredPlace,
                    petSummary,
                    temperature,
                    humidity,
                    precipitationType,
                    rainfall,
                    windSpeed,
                    yesNo(raining),
                    yesNo(cold),
                    yesNo(hot),
                    yesNo(windy),
                    walkLevel,
                    weatherSummary,
                    placeSection,
                    ragSection
            );
    log.info("[프롬프트] 프롬프트 생성 완료 length={}, rankedCount={}, ragLength={}",
            prompt.length(),
            rankedPlaces == null ? 0 : rankedPlaces.size(),
            ragContext == null ? 0 : ragContext.length());
    log.debug("[프롬프트] 프롬프트 미리보기 value={}", shorten(prompt, 240));
    return prompt;
  }

  /** 현재 날씨 상태를 추천 문장용 한 줄 요약으로 변환한다. */
  private String buildWeatherSummary(WeatherContext weather) {
    if (weather == null || weather.getWalkLevel() == null) {
      return "날씨 정보를 충분히 해석하기 어려운 상태입니다.";
    }

    String currentWalkLevel = weather.getWalkLevel();

    if ("GOOD".equalsIgnoreCase(currentWalkLevel)) {
      return "야외 활동도 무난하게 가능한 날씨입니다.";
    }
    if ("CAUTION".equalsIgnoreCase(currentWalkLevel)) {
      return "야외 활동은 가능하지만 주의가 필요해 실내나 혼합형 장소를 함께 고려하는 편이 좋습니다.";
    }
    if ("DANGEROUS".equalsIgnoreCase(currentWalkLevel)) {
      return "야외 활동 부담이 큰 날씨라 실내 중심 장소가 더 적합합니다.";
    }
    return "날씨 상태를 종합하면 무리 없는 동선 위주로 추천하는 편이 적절합니다.";
  }

  /** 반려견 특성을 짧은 자연어 요약으로 정리한다. */
  private String buildPetSummary(Pet pet) {
    List<String> parts = new ArrayList<>();

    if (pet == null) {
      return "반려견 정보가 충분하지 않습니다.";
    }

    if (pet.getPetBreed() != null) {
      parts.add(pet.getPetBreed() + " 품종입니다");
    }
    if (pet.getPetSize() != null) {
      parts.add("크기는 " + pet.getPetSize() + "입니다");
    }
    if (pet.getPersonality() != null && !pet.getPersonality().isBlank()) {
      parts.add("성향은 " + pet.getPersonality() + " 편입니다");
    }

    if (parts.isEmpty()) {
      return "반려견 정보가 충분하지 않습니다.";
    }

    return String.join(", ", parts) + ".";
  }

  /** 반려견 선호 장소를 비어 있지 않은 문자열로 반환한다. */
  private String safePreferredPlace(Pet pet) {
    if (pet == null || pet.getPreferredPlace() == null || pet.getPreferredPlace().isBlank()) {
      return "정보 없음";
    }
    return pet.getPreferredPlace();
  }

  /** 상위 3개 장소의 핵심 점수와 차이를 프롬프트 입력 블록으로 조립한다. */
  private String buildPlaceSection(List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "- 추천 가능한 장소 후보가 없습니다.";
    }

    StringBuilder sb = new StringBuilder();
    int rank = 1;

    for (ScoredPlace scoredPlace : rankedPlaces.stream().limit(3).toList()) {
      Place place = scoredPlace.getPlace();
      String placeType = inferPlaceType(place);
      String scenario = inferRecommendedScenario(place, scoredPlace);

      sb.append(rank).append("위 ").append(nullSafe(place.getTitle())).append("\n")
              .append("- 카테고리: ").append(nullSafe(place.getCategory())).append("\n")
              .append("- 공간 성격: ").append(placeType).append("\n")
              .append("- 태그: ").append(shorten(nullSafe(place.getTags()), 100)).append("\n")
              .append("- 총점: ").append(scoredPlace.getTotalScore()).append("\n")
              .append("- 반려동물 적합도: ").append(scoredPlace.getDogFitScore()).append("\n")
              .append("- 날씨 적합도: ").append(scoredPlace.getWeatherScore()).append("\n")
              .append("- 장소 환경 점수: ").append(scoredPlace.getPlaceEnvScore()).append("\n")
              .append("- 거리/이동 편의: ").append(scoredPlace.getDistanceScore()).append("\n")
              .append("- 추가 요소 점수: ").append(scoredPlace.getHistoryScore()).append("\n")
              .append("- 감점: ").append(scoredPlace.getPenaltyScore()).append("\n")
              .append("- 추천 상황: ").append(scenario).append("\n")
              .append("- 점수 요약: ").append(nullSafe(scoredPlace.getSummary())).append("\n")
              .append("- 기존 추천 이유: ").append(nullSafe(scoredPlace.getReason())).append("\n\n");

      rank++;
    }

    return sb.toString().trim();
  }

  /** RAG 검색 결과를 추천에 직접 쓸 수 있는 짧은 참고 블록으로 정리한다. */
  private String buildRagSection(String ragContext) {
    String normalized = shorten(nullSafe(ragContext), 400);
    if ("정보 없음".equals(normalized)) {
      return "- 참고할 추가 문맥 없음";
    }
    return """
            - 아래 내용은 일반 설명이 아니라 현재 추천에 필요한 주의점만 골라 반영한다.
            - 직접 관련 있는 내용만 1회 정도 자연스럽게 녹여라.
            %s
            """.formatted(normalized).trim();
  }

  /** 장소 설명과 태그를 바탕으로 실내/야외 성격을 추정한다. */
  private String inferPlaceType(Place place) {
    String searchable = ((place.getCategory() == null ? "" : place.getCategory()) + " "
            + (place.getDescription() == null ? "" : place.getDescription()) + " "
            + (place.getTags() == null ? "" : place.getTags())).toLowerCase();

    if (containsAny(searchable, "실내", "카페", "라운지", "전시", "박물관")) {
      return "실내 중심";
    }
    if (containsAny(searchable, "야외", "공원", "산책", "정원", "테라스")) {
      return "야외 중심";
    }
    return "실내외 혼합 또는 정보 제한";
  }

  /** 장소 점수 성격에 맞는 이용 상황을 짧게 요약한다. */
  private String inferRecommendedScenario(Place place, ScoredPlace scoredPlace) {
    String placeType = inferPlaceType(place);

    if ("실내 중심".equals(placeType) && scoredPlace.getWeatherScore() >= scoredPlace.getDistanceScore()) {
      return "날씨 변수나 컨디션 부담이 있을 때 안정적으로 가기 좋음";
    }
    if ("야외 중심".equals(placeType) && scoredPlace.getDogFitScore() >= scoredPlace.getWeatherScore()) {
      return "반려견이 활동적으로 움직이기 좋은 날에 만족도가 높음";
    }
    if (scoredPlace.getDistanceScore() >= 7.0) {
      return "멀리 이동하지 않고 가볍게 다녀오고 싶을 때 적합함";
    }
    return "취향과 상황을 균형 있게 맞추고 싶을 때 무난한 선택";
  }

  /** 텍스트에 주어진 키워드 중 하나라도 포함되는지 확인한다. */
  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /** boolean 값을 프롬프트용 예/아니오 문자열로 변환한다. */
  private String yesNo(boolean value) {
    return value ? "예" : "아니오";
  }

  /** null 또는 빈 문자열을 기본 문구로 치환한다. */
  private String nullSafe(String value) {
    return value == null || value.isBlank() ? "정보 없음" : value;
  }

  /** 문자열이 너무 길면 지정 길이까지 자른다. */
  private String shorten(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "정보 없음";
    }
    return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
  }
}
