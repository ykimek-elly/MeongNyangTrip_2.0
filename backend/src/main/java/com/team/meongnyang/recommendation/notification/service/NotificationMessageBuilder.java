package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {

  private static final String DEFAULT_COMMENT = "오늘은 날씨와 아이 성향을 함께 보고 부담이 덜한 쪽을 우선 골랐어요.";
  private static final int MAX_COMMENT_LENGTH = 78;
  private static final int HANGUL_BASE = 0xAC00;
  private static final int HANGUL_LAST = 0xD7A3;
  private static final int HANGUL_JONGSEONG_CYCLE = 28;

  /**
   * 알림 템플릿 빌더
   * @param user
   * @param pet
   * @param place
   * @param message
   * @param weatherType
   * @return
   */
  public String buildMessage(User user, Pet pet, Place place, String message, String weatherType) {
    return buildMessage(user, pet, place, message, weatherType, null);
  }

  public String buildMessage(User user, Pet pet, Place place, String message, String weatherType, String weatherWalkLevel) {
    String petName = resolvePetName(pet);
    String weatherLine = resolveWeatherLine(weatherType);
    String intro = resolveIntro(weatherType, petName);
    String placeTitle = place != null && place.getTitle() != null && !place.getTitle().isBlank()
            ? place.getTitle()
            : "추천 장소";
    String comment = resolveComment(pet, place, message, petName, placeTitle, weatherType, weatherWalkLevel);
    String outro = resolveOutro(weatherType, petName);

    return """
            %s 오늘의 반려생활 알림이에요! 🐾

            %s
            %s

            📍 %s에게 잘 맞는 장소 : %s
            💬 %s

            %s
            멍냥트립이 더 가볍게 함께할게요.
            """.formatted(
            petName,
            weatherLine,
            intro,
            petName,
            placeTitle,
            comment,
            outro
    ).trim();
  }

  private String resolvePetName(Pet pet) {
    if (pet == null || pet.getPetName() == null || pet.getPetName().isBlank()) {
      return "우리 아이";
    }
    return stripTrailingJosa(pet.getPetName());
  }

  private String resolveWeatherLine(String weatherType) {
    return switch (weatherType) {
      case "RAINY" -> "☔ 오늘의 날씨 : 비";
      case "CLOUDY" -> "☁️ 오늘의 날씨 : 흐림";
      case "HEATWAVE" -> "🌡️ 오늘의 날씨 : 더위";
      case "COLD_WAVE" -> "🧣 오늘의 날씨 : 추위";
      default -> "🌤️ 오늘의 날씨 : 맑음";
    };
  }

  private String resolveIntro(String weatherType, String petName) {
    return switch (weatherType) {
      case "RAINY" -> "%s 무리 없이 움직일 수 있는 쪽을 먼저 살펴봤어요.".formatted(withJosa(petName, "와", "과"));
      case "CLOUDY" -> "%s 취향과 오늘 컨디션을 함께 보고 추렸어요.".formatted(withPossessiveJosa(petName));
      case "HEATWAVE" -> "%s 더위에 오래 노출되지 않도록 동선을 먼저 봤어요.".formatted(withSubjectJosa(petName));
      case "COLD_WAVE" -> "%s 추위에 오래 머물지 않도록 체류 부담이 적은 곳을 우선 봤어요.".formatted(withSubjectJosa(petName));
      default -> "%s 성향과 오늘 컨디션을 함께 보고 골라봤어요.".formatted(withPossessiveJosa(petName));
    };
  }

  private String resolveComment(
          Pet pet,
          Place place,
          String message,
          String petName,
          String placeTitle,
          String weatherType,
          String weatherWalkLevel
  ) {
    String generatedComment = buildReasonComment(pet, place, petName, weatherType, weatherWalkLevel);
    if (!generatedComment.isBlank()) {
      return generatedComment;
    }

    if (message == null || message.isBlank()) {
      return DEFAULT_COMMENT;
    }

    String sanitizedComment = sanitizeAiComment(message, petName, placeTitle);
    if (sanitizedComment.isBlank()) {
      return DEFAULT_COMMENT;
    }

    return trimToLength(extractFirstSentence(sanitizedComment), MAX_COMMENT_LENGTH);
  }

  private String buildReasonComment(Pet pet, Place place, String petName, String weatherType, String weatherWalkLevel) {
    String primaryReason = resolvePrimaryReason(pet, place, petName, weatherType, weatherWalkLevel);
    String weatherReason = resolveWeatherReason(place, weatherType, weatherWalkLevel);
    return composeComment(primaryReason, weatherReason);
  }

  private String resolvePrimaryReason(Pet pet, Place place, String petName, String weatherType, String weatherWalkLevel) {
    String preferredPlaceReason = buildPreferredPlaceReason(pet, place, petName, weatherType, weatherWalkLevel);
    if (!preferredPlaceReason.isBlank()) {
      return preferredPlaceReason;
    }

    String activityReason = buildActivityReason(pet, place, petName, weatherType, weatherWalkLevel);
    if (!activityReason.isBlank()) {
      return activityReason;
    }

    String personalityReason = buildPersonalityReason(pet, place, petName);
    if (!personalityReason.isBlank()) {
      return personalityReason;
    }

    return buildPlaceFeatureReason(place, petName);
  }

  private String buildPreferredPlaceReason(Pet pet, Place place, String petName, String weatherType, String weatherWalkLevel) {
    if (pet == null || place == null) {
      return "";
    }

    PreferenceType preferredType = resolvePreferenceType(pet.getPreferredPlace());
    if (preferredType == PreferenceType.UNKNOWN) {
      return "";
    }

    PreferenceMatch match = matchPreference(preferredType, place);
    return switch (match) {
      case EXACT -> buildExactPreferenceReason(preferredType, petName);
      case PARTIAL -> buildPartialPreferenceReason(preferredType, petName);
      case MISMATCH -> buildMismatchPreferenceReason(preferredType, petName, pet, place, weatherType, weatherWalkLevel);
      case UNSUPPORTED -> "";
    };
  }

  private String buildExactPreferenceReason(PreferenceType preferredType, String petName) {
    return switch (preferredType) {
      case INDOOR_CAFE -> "%s 실내 선호를 반영해 오래 머물기 편한 쪽으로 골랐어요".formatted(withPossessiveJosa(petName));
      case WALK_TRAIL -> "%s 산책로 선호를 반영해 움직임이 자연스러운 쪽으로 골랐어요".formatted(withPossessiveJosa(petName));
      case EXHIBITION -> "%s 전시 선호를 반영해 실내 관람 흐름이 자연스러운 쪽으로 골랐어요".formatted(withPossessiveJosa(petName));
      case UNKNOWN -> "";
    };
  }

  private String buildPartialPreferenceReason(PreferenceType preferredType, String petName) {
    return switch (preferredType) {
      case INDOOR_CAFE -> "%s 실내 선호를 일부 고려해 머무름 부담이 적은 쪽으로 맞췄어요".formatted(withPossessiveJosa(petName));
      case WALK_TRAIL -> "%s 산책 성향을 일부 고려해 이동이 답답하지 않은 쪽으로 맞췄어요".formatted(withPossessiveJosa(petName));
      case EXHIBITION -> "%s 전시 선호를 일부 고려해 관람 흐름이 편한 쪽으로 맞췄어요".formatted(withPossessiveJosa(petName));
      case UNKNOWN -> "";
    };
  }

  private String buildMismatchPreferenceReason(
          PreferenceType preferredType,
          String petName,
          Pet pet,
          Place place,
          String weatherType,
          String weatherWalkLevel
  ) {
    if (shouldMentionPreferenceMismatch(preferredType, pet, place, weatherType, weatherWalkLevel)) {
      return "%s 선호도 고려했지만 오늘은 날씨와 장소 적합도를 더 우선했어요".formatted(withPossessiveJosa(petName));
    }
    return "";
  }

  private boolean shouldMentionPreferenceMismatch(
          PreferenceType preferredType,
          Pet pet,
          Place place,
          String weatherType,
          String weatherWalkLevel
  ) {
    if (preferredType == PreferenceType.UNKNOWN || place == null) {
      return false;
    }

    if ("RAINY".equals(weatherType) || "HEATWAVE".equals(weatherType) || "COLD_WAVE".equals(weatherType)) {
      return true;
    }

    if (pet != null && pet.getPetActivity() == Pet.PetActivity.HIGH && isTrailLike(place)) {
      return true;
    }

    if (pet != null && pet.getPetActivity() == Pet.PetActivity.LOW && isIndoorLike(place)) {
      return true;
    }

    String normalizedWalkLevel = normalize(weatherWalkLevel);
    return "dangerous".equals(normalizedWalkLevel) || "caution".equals(normalizedWalkLevel);
  }

  private String buildActivityReason(Pet pet, Place place, String petName, String weatherType, String weatherWalkLevel) {
    if (pet == null || pet.getPetActivity() == null) {
      return "";
    }

    return switch (pet.getPetActivity()) {
      case HIGH -> buildHighActivityReason(place, petName, weatherType, weatherWalkLevel);
      case LOW -> "%s 무리하지 않도록 오래 머물기 쉬운 쪽을 우선 봤어요".formatted(withSubjectJosa(petName));
      case NORMAL -> "";
    };
  }

  private String buildHighActivityReason(Place place, String petName, String weatherType, String weatherWalkLevel) {
    if (isTrailLike(place) && isOutdoorFriendly(weatherType, weatherWalkLevel)) {
      return "활동량이 높은 %s 맞게 움직임이 이어지는 동선을 우선 봤어요".formatted(withJosa(petName, "에", "에게"));
    }

    if (isOutdoorFriendly(weatherType, weatherWalkLevel)) {
      return "활동량이 높은 %s 답답하지 않게 움직일 수 있는 쪽으로 골랐어요".formatted(withSubjectJosa(petName));
    }

    return "활동량은 살리되 날씨 부담은 덜도록 동선이 무겁지 않은 쪽을 골랐어요";
  }

  private String buildPersonalityReason(Pet pet, Place place, String petName) {
    if (pet == null) {
      return "";
    }

    String personality = normalize(pet.getPersonality());
    if (personality.isBlank()) {
      return "";
    }

    if (isCalmPersonality(personality)) {
      return "차분한 성향의 %s 오래 머물러도 부담이 적은 쪽으로 골랐어요".formatted(petName);
    }

    if (isEnergeticPersonality(personality)) {
      return "활발한 성향의 %s 지루하지 않게 머물 수 있는 흐름을 반영했어요".formatted(petName);
    }

    if (isSocialPersonality(personality) && isCafeLike(place)) {
      return "%s 사교적인 성향을 고려해 머무름이 자연스러운 공간으로 맞췄어요".formatted(withPossessiveJosa(petName));
    }

    return "";
  }

  private String buildPlaceFeatureReason(Place place, String petName) {
    String placeFeature = describePlaceFeature(place);
    if (placeFeature.isBlank()) {
      return "";
    }
    return "%s %s 흐름이 더 잘 맞는 쪽으로 골랐어요".formatted(withJosa(petName, "에겐", "에겐"), placeFeature);
  }

  private String resolveWeatherReason(Place place, String weatherType, String weatherWalkLevel) {
    if ("RAINY".equals(weatherType)) {
      return "비를 피하면서 동선 부담을 줄일 수 있는 쪽을 우선 봤어요";
    }

    if ("HEATWAVE".equals(weatherType)) {
      return isIndoorLike(place)
              ? "더위에 오래 노출되지 않도록 실내 중심으로 맞췄어요"
              : "더위를 오래 버티지 않게 체류 부담이 덜한 쪽을 우선 봤어요";
    }

    if ("COLD_WAVE".equals(weatherType)) {
      return "추위에 오래 노출되지 않도록 체류 부담이 덜한 쪽을 우선 봤어요";
    }

    String normalizedWalkLevel = normalize(weatherWalkLevel);
    if ("dangerous".equals(normalizedWalkLevel)) {
      return "야외 이동 부담이 커서 무리 없는 동선을 우선 봤어요";
    }
    if ("caution".equals(normalizedWalkLevel)) {
      return "야외 이동은 가능하지만 무리 없도록 균형을 먼저 봤어요";
    }

    if ("CLOUDY".equals(weatherType)) {
      return "날씨가 무겁지 않아 성향과 활동 흐름을 함께 반영했어요";
    }

    if ("good".equals(normalizedWalkLevel)) {
      return "야외 이동이 가능한 컨디션이라 활동 흐름까지 함께 반영했어요";
    }

    return "";
  }

  private String composeComment(String primaryReason, String weatherReason) {
    String firstSentence = ensureSentence(primaryReason);
    String secondSentence = ensureSentence(weatherReason);

    if (firstSentence.isBlank() && secondSentence.isBlank()) {
      return DEFAULT_COMMENT;
    }
    if (firstSentence.isBlank()) {
      return fitSingleSentence(secondSentence);
    }
    if (secondSentence.isBlank()) {
      return fitSingleSentence(firstSentence);
    }

    String combined = firstSentence + " " + secondSentence;
    if (combined.length() <= MAX_COMMENT_LENGTH) {
      return combined;
    }
    if (firstSentence.length() <= MAX_COMMENT_LENGTH) {
      return firstSentence;
    }
    if (secondSentence.length() <= MAX_COMMENT_LENGTH) {
      return secondSentence;
    }
    return DEFAULT_COMMENT;
  }

  private String fitSingleSentence(String sentence) {
    if (sentence == null || sentence.isBlank()) {
      return DEFAULT_COMMENT;
    }
    return sentence.length() <= MAX_COMMENT_LENGTH ? sentence : DEFAULT_COMMENT;
  }

  private PreferenceType resolvePreferenceType(String preferredPlace) {
    String normalized = normalize(preferredPlace);
    if (normalized.isBlank()) {
      return PreferenceType.UNKNOWN;
    }

    if (normalized.contains("실내") || normalized.contains("카페") || normalized.contains("브런치")) {
      return PreferenceType.INDOOR_CAFE;
    }
    if (normalized.contains("산책로") || normalized.contains("산책") || normalized.contains("공원")) {
      return PreferenceType.WALK_TRAIL;
    }
    if (normalized.contains("전시") || normalized.contains("미술관") || normalized.contains("박물관") || normalized.contains("갤러리")) {
      return PreferenceType.EXHIBITION;
    }
    return PreferenceType.UNKNOWN;
  }

  private PreferenceMatch matchPreference(PreferenceType preferredType, Place place) {
    if (preferredType == PreferenceType.UNKNOWN || place == null) {
      return PreferenceMatch.UNSUPPORTED;
    }

    return switch (preferredType) {
      case INDOOR_CAFE -> matchIndoorCafe(place);
      case WALK_TRAIL -> matchWalkTrail(place);
      case EXHIBITION -> matchExhibition(place);
      case UNKNOWN -> PreferenceMatch.UNSUPPORTED;
    };
  }

  private PreferenceMatch matchIndoorCafe(Place place) {
    if (isCafeLike(place)) {
      return PreferenceMatch.EXACT;
    }
    if (isIndoorLike(place) || isStayLike(place)) {
      return PreferenceMatch.PARTIAL;
    }
    return PreferenceMatch.MISMATCH;
  }

  private PreferenceMatch matchWalkTrail(Place place) {
    if (isTrailLike(place)) {
      return PreferenceMatch.EXACT;
    }
    if (isOutdoorLike(place)) {
      return PreferenceMatch.PARTIAL;
    }
    return PreferenceMatch.MISMATCH;
  }

  private PreferenceMatch matchExhibition(Place place) {
    if (isExhibitionLike(place)) {
      return PreferenceMatch.EXACT;
    }
    if (isIndoorLike(place)) {
      return PreferenceMatch.PARTIAL;
    }
    return PreferenceMatch.MISMATCH;
  }

  private String describePlaceFeature(Place place) {
    if (place == null) {
      return "";
    }
    if (isTrailLike(place)) {
      return "산책";
    }
    if (isExhibitionLike(place)) {
      return "관람";
    }
    if (isCafeLike(place)) {
      return "머무름";
    }
    if (isStayLike(place)) {
      return "체류";
    }
    if (isIndoorLike(place)) {
      return "실내";
    }
    return "";
  }

  private boolean isCalmPersonality(String personality) {
    return personality.contains("차분")
            || personality.contains("조용")
            || personality.contains("예민")
            || personality.contains("소심")
            || personality.contains("겁");
  }

  private boolean isEnergeticPersonality(String personality) {
    return personality.contains("활발")
            || personality.contains("에너지")
            || personality.contains("장난")
            || personality.contains("명랑");
  }

  private boolean isSocialPersonality(String personality) {
    return personality.contains("사교")
            || personality.contains("친화")
            || personality.contains("사람");
  }

  private boolean isOutdoorFriendly(String weatherType, String weatherWalkLevel) {
    if ("RAINY".equals(weatherType) || "HEATWAVE".equals(weatherType) || "COLD_WAVE".equals(weatherType)) {
      return false;
    }
    String normalizedWalkLevel = normalize(weatherWalkLevel);
    return normalizedWalkLevel.isBlank() || "good".equals(normalizedWalkLevel) || "caution".equals(normalizedWalkLevel);
  }

  private boolean isIndoorLike(Place place) {
    String normalizedCategory = normalize(place == null ? null : place.getCategory());
    String searchable = searchablePlaceText(place);
    return "dining".equals(normalizedCategory)
            || hasAnyKeyword(searchable, "실내", "카페", "브런치", "라운지", "휴식", "관람", "전시");
  }

  private boolean isOutdoorLike(Place place) {
    return hasAnyKeyword(searchablePlaceText(place), "야외", "공원", "산책", "산책로", "둘레길", "수변");
  }

  private boolean isTrailLike(Place place) {
    String normalizedCategory = normalize(place == null ? null : place.getCategory());
    String searchable = searchablePlaceText(place);
    return "place".equals(normalizedCategory)
            && hasAnyKeyword(searchable, "산책", "공원", "산책로", "둘레길", "야외", "수변");
  }

  private boolean isExhibitionLike(Place place) {
    return hasAnyKeyword(searchablePlaceText(place), "전시", "전시관", "미술관", "박물관", "갤러리", "관람");
  }

  private boolean isCafeLike(Place place) {
    return hasAnyKeyword(searchablePlaceText(place), "카페", "브런치", "라운지", "디저트");
  }

  private boolean isStayLike(Place place) {
    String normalizedCategory = normalize(place == null ? null : place.getCategory());
    String searchable = searchablePlaceText(place);
    return "stay".equals(normalizedCategory)
            || hasAnyKeyword(searchable, "숙소", "호텔", "펜션", "스테이");
  }

  private String searchablePlaceText(Place place) {
    if (place == null) {
      return "";
    }

    return normalize(place.getCategory()) + " "
            + normalize(place.getTitle()) + " "
            + normalize(place.getDescription()) + " "
            + normalize(place.getTags());
  }

  private boolean hasAnyKeyword(String text, String... keywords) {
    String normalizedText = normalize(text);
    for (String keyword : keywords) {
      String normalizedKeyword = normalize(keyword);
      if (!normalizedKeyword.isBlank() && normalizedText.contains(normalizedKeyword)) {
        return true;
      }
    }
    return false;
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.trim().toLowerCase().replace(" ", "");
  }

  private String ensureSentence(String text) {
    String normalized = text == null ? "" : text.trim();
    if (normalized.isBlank()) {
      return "";
    }
    return endsWithSentenceMark(normalized) ? normalized : normalized + ".";
  }

  private String sanitizeAiComment(String message, String petName, String placeTitle) {
    String sanitized = message
            .replace("\r", " ")
            .replace("\n", " ")
            .trim();

    sanitized = removeLeadingPhrase(sanitized, "오늘 날씨");
    sanitized = removeLeadingPhrase(sanitized, "추천 장소");
    sanitized = removeLeadingPhrase(sanitized, "멍냥트립");
    sanitized = removeLeadingPhrase(sanitized, petName);
    sanitized = removeLeadingPhrase(sanitized, placeTitle);

    sanitized = sanitized.replaceAll("\\s+", " ");
    sanitized = sanitized.replaceAll("^[\\p{Punct}\\s]+", "");
    sanitized = sanitized.replaceAll("[\\p{Punct}\\s]+$", "");
    return sanitized.trim();
  }

  private String removeLeadingPhrase(String text, String phrase) {
    if (phrase == null || phrase.isBlank()) {
      return text;
    }

    String normalized = text.trim();
    return normalized.startsWith(phrase) ? normalized.substring(phrase.length()).trim() : normalized;
  }

  private String extractFirstSentence(String message) {
    String firstLine = message.trim();
    for (String delimiter : new String[]{". ", "! ", "? ", ".\n", "!\n", "?\n", ".", "!", "?"}) {
      int delimiterIndex = firstLine.indexOf(delimiter);
      if (delimiterIndex >= 0) {
        return firstLine.substring(0, delimiterIndex + 1).trim();
      }
    }
    return firstLine;
  }

  private String trimToLength(String message, int maxLength) {
    String normalized = ensureSentence(message);
    if (normalized.isBlank()) {
      return DEFAULT_COMMENT;
    }
    return normalized.length() <= maxLength ? normalized : DEFAULT_COMMENT;
  }

  private boolean endsWithSentenceMark(String text) {
    return text.endsWith(".") || text.endsWith("!") || text.endsWith("?");
  }

  private String resolveOutro(String weatherType, String petName) {
    return switch (weatherType) {
      case "RAINY" -> "%s 무리 없는 하루가 되길 바랄게요.".formatted(withJosa(petName, "와", "과"));
      case "CLOUDY" -> "%s 가볍게 움직이기 좋은 흐름으로 이어지면 좋겠어요.".formatted(withJosa(petName, "와", "과"));
      case "HEATWAVE" -> "%s 더위에 지치지 않게 천천히 다녀오세요.".formatted(withSubjectJosa(petName));
      case "COLD_WAVE" -> "%s 추위에 무리하지 않는 하루가 되길 바랄게요.".formatted(withSubjectJosa(petName));
      default -> "%s 오늘 컨디션에 맞는 속도로 다녀오세요.".formatted(withJosa(petName, "와", "과"));
    };
  }

  private String withPossessiveJosa(String word) {
    return stripTrailingJosa(word) + "의";
  }

  private String withSubjectJosa(String word) {
    String normalized = stripTrailingJosa(word);
    return normalized + resolveJosa(normalized, "가", "이");
  }

  private String withJosa(String word, String withoutBatchim, String withBatchim) {
    if (word == null || word.isBlank()) {
      return word;
    }
    String normalized = stripTrailingJosa(word);
    return normalized + resolveJosa(normalized, withoutBatchim, withBatchim);
  }

  private String stripTrailingJosa(String word) {
    if (word == null) {
      return "";
    }

    String normalized = word.trim();
    String[] josas = {"의", "와", "과", "이", "가", "은", "는", "을", "를"};
    for (String josa : josas) {
      if (normalized.endsWith(josa) && normalized.length() > josa.length()) {
        return normalized.substring(0, normalized.length() - josa.length());
      }
    }
    return normalized;
  }

  private String resolveJosa(String word, String withoutBatchim, String withBatchim) {
    if (word == null || word.isBlank()) {
      return withoutBatchim;
    }

    char lastChar = word.charAt(word.length() - 1);
    if (!isHangulSyllable(lastChar)) {
      return withoutBatchim;
    }
    return hasBatchim(lastChar) ? withBatchim : withoutBatchim;
  }

  private boolean isHangulSyllable(char ch) {
    return ch >= HANGUL_BASE && ch <= HANGUL_LAST;
  }

  private boolean hasBatchim(char ch) {
    return (ch - HANGUL_BASE) % HANGUL_JONGSEONG_CYCLE != 0;
  }

  private enum PreferenceType {
    INDOOR_CAFE,
    WALK_TRAIL,
    EXHIBITION,
    UNKNOWN
  }

  private enum PreferenceMatch {
    EXACT,
    PARTIAL,
    MISMATCH,
    UNSUPPORTED
  }
}
