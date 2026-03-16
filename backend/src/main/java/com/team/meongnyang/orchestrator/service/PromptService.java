package com.team.meongnyang.orchestrator.service;

import com.team.meongnyang.orchestrator.dto.ScoredPlace;
import com.team.meongnyang.weather.dto.WeatherContext;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptService {
  public String buildRecommendationPrompt(
          User user,
          Dog dog,
          WeatherContext weather,
          List<ScoredPlace> rankedPlaces,
          String ragContext
  ) {
    String nickname = user != null ? nullSafe(user.getNickname()) : "정보 없음";
    String dogName = dog != null ? nullSafe(dog.getDogName()) : "정보 없음";
    String dogBreed = dog != null ? nullSafe(dog.getDogBreed()) : "정보 없음";
    String dogSize = dog != null && dog.getDogSize() != null ? String.valueOf(dog.getDogSize()) : "정보 없음";
    String dogPersonality = dog != null ? nullSafe(dog.getPersonality()) : "정보 없음";
    String preferredPlace = safePreferredPlace(dog);

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

    String placeSection = buildPlaceSection(rankedPlaces);
    String weatherSummary = buildWeatherSummary(weather);
    String dogSummary = buildDogSummary(dog);

    return """
                너는 반려견 동반 외출 추천 도우미다.
                아래 사용자 정보, 반려견 정보, 현재 날씨, 추천 장소 점수 결과, 참고 문서를 바탕으로
                자연스럽고 실용적인 추천 문장을 작성해라.

                반드시 지켜야 할 규칙:
                1. 아래 [참고 문서] 내용을 반드시 우선 참고할 것
                2. 참고 문서에 없는 내용은 추측하거나 과장하지 말 것
                3. 추천 장소는 반드시 [추천 장소 후보]에 포함된 장소명만 사용할 것
                4. 추천 장소는 점수가 높은 순서대로 우선 언급할 것
                5. 추천 우선순위는 이미 계산된 점수 결과를 따르며, 임의로 순서를 바꾸지 말 것
                6. 현재 날씨와 반려견 특성을 함께 고려한 추천이어야 할 것
                7. 날씨가 좋지 않으면 무리한 야외활동을 권장하지 말 것
                8. 주의사항 또는 돌봄 팁을 1문장 이상 포함할 것
                9. 답변은 한국어로 작성할 것
                10. 말투는 부드럽고 친절하며 실용적으로 작성할 것
                11. 전체 답변은 3~5문장으로 작성할 것
                12. 장소 이름은 최대 3곳까지만 언급할 것
                13. 제공되지 않은 장소, 정보, 효능, 의학적 판단은 만들어내지 말 것

                [사용자 정보]
                닉네임: %s

                [반려견 정보]
                이름: %s
                견종: %s
                크기: %s
                성향: %s
                선호 장소: %s
                반려견 요약: %s

                [현재 날씨]
                기온: %.1f℃
                습도: %d%%
                강수 형태: %s
                강수량: %.1fmm
                풍속: %.1fm/s
                강수 여부: %s
                저온 여부: %s
                고온 여부: %s
                강풍 여부: %s
                산책 가능 수준: %s
                날씨 해석: %s

                [추천 장소 후보]
                %s

                [참고 문서]
                %s

                [응답 목표]
                - 현재 날씨에 대한 짧은 판단
                - 반려견 상태를 고려한 한 줄 조언
                - 상위 추천 장소 1~3곳 제안
                - 필요 시 주의사항 1문장 추가
                """
            .formatted(
                    nickname,
                    dogName,
                    dogBreed,
                    dogSize,
                    dogPersonality,
                    preferredPlace,
                    dogSummary,
                    temperature,
                    humidity,
                    precipitationType,
                    rainfall,
                    windSpeed,
                    raining,
                    cold,
                    hot,
                    windy,
                    walkLevel,
                    weatherSummary,
                    placeSection,
                    shorten(nullSafe(ragContext), 800)
            );
  }


  private String buildWeatherSummary(WeatherContext weather) {
    if (weather == null || weather.getWalkLevel() == null) {
      return "날씨 정보를 충분히 해석하기 어려운 상태입니다.";
    }

    String walkLevel = weather.getWalkLevel();

    if ("GOOD".equalsIgnoreCase(walkLevel)) {
      return "야외 활동이 비교적 무난한 날씨입니다.";
    }
    if ("CAUTION".equalsIgnoreCase(walkLevel)) {
      return "야외 활동 시 주의가 필요하며, 실내 장소를 우선 고려하는 것이 좋습니다.";
    }
    if ("DANGEROUS".equalsIgnoreCase(walkLevel)) {
      return "야외 활동은 가급적 피하고 실내 위주로 고려하는 것이 좋습니다.";
    }
    return "날씨 상태를 종합적으로 살펴 무리 없는 활동을 추천해야 합니다.";
  }

  private String buildDogSummary(Dog dog) {
    List<String> parts = new ArrayList<>();

    if (dog == null) {
      return "반려견 정보가 충분하지 않습니다.";
    }

    if (dog.getDogBreed() != null) {
      parts.add(dog.getDogBreed() + " 견종입니다");
    }
    if (dog.getDogSize() != null) {
      parts.add("크기는 " + dog.getDogSize() + "입니다");
    }
    if (dog.getPersonality() != null && !dog.getPersonality().isBlank()) {
      parts.add("성향은 " + dog.getPersonality() + " 편입니다");
    }

    if (parts.isEmpty()) {
      return "반려견 정보가 충분하지 않습니다.";
    }

    return String.join(", ", parts) + ".";
  }

  private String safePreferredPlace(Dog dog) {
    if (dog == null || dog.getPreferredPlace() == null || dog.getPreferredPlace().isBlank()) {
      return "정보 없음";
    }
    return dog.getPreferredPlace();
  }

  /**
   * 추천 장소 목록을 프롬프트용 문자열로 변환한다.
   *
   * 정책:
   * - 상위 3개까지만 사용
   * - 장소명, 카테고리, 위치, 평점, 시설 정보 중심으로 요약
   *
   * @param rankedPlaces 점수순 정렬된 장소 목록
   * @return 장소 정보 문자열
   */
  private String buildPlaceSection(List<ScoredPlace> rankedPlaces) {
    if (rankedPlaces == null || rankedPlaces.isEmpty()) {
      return "추천 가능한 장소 후보가 없습니다.";
    }

    StringBuilder sb = new StringBuilder();

    int rank = 1;

    for (ScoredPlace scoredPlace : rankedPlaces.stream().limit(3).toList()) {
      Place place = scoredPlace.getPlace();

      sb.append(rank).append("위. ")
              .append(nullSafe(place.getTitle())).append("\n")
              .append("- 유형: ").append(nullSafe(place.getCategory())).append("\n")
              .append("- 태그: ").append(shorten(nullSafe(place.getTags()), 80)).append("\n")
              .append("- 총점: ").append(scoredPlace.getTotalScore()).append("\n")
              .append("- 반려견 적합도: ").append(scoredPlace.getDogFitScore()).append("\n")
              .append("- 날씨 적합도: ").append(scoredPlace.getWeatherScore()).append("\n")
              .append("- 거리 점수: ").append(scoredPlace.getDistanceScore()).append("\n")
              .append("- 추천 이유: ").append(nullSafe(scoredPlace.getReason())).append("\n\n");

      rank++;
    }

    return sb.toString();
  }

  /**
   * null 문자열을 안전하게 처리한다.
   *
   * @param value 원본 문자열
   * @return null 이면 "정보 없음", 아니면 원본 값
   */
  private String nullSafe(String value) {
    return value == null || value.isBlank() ? "정보 없음" : value;
  }

  /**
   * 시설 정보를 프롬프트에 넣기 좋게 정리한다.
   *
   * 처리 정책:
   * - null / blank 이면 "정보 없음"
   * - 숫자만 있으면 "정보 없음"
   * - 너무 길면 50자까지만 사용
   *
   * @param facilityInfo 시설 정보 원본 값
   * @return 정리된 시설 정보 문자열
   */
  private String formatFacilityInfo(String facilityInfo) {
    if (facilityInfo == null || facilityInfo.isBlank()) {
      return "정보 없음";
    }

    if (facilityInfo.matches("\\d+")) {
      return "정보 없음";
    }

    return shorten(facilityInfo, 50);
  }

  private String shorten(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "정보 없음";
    }
    return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
  }


}
