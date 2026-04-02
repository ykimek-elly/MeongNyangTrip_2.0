package com.team.meongnyang.recommendation.util;

import java.util.Locale;

public final class RecommendationTextUtils {

    private RecommendationTextUtils() {
    }

    /** 빈 문자열이면 기본값으로 대체한다. */
    public static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** 여러 줄 텍스트를 한 줄로 정리한다. */
    public static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    /** 문자열을 지정 길이로 축약한다. */
    public static String abbreviate(String value, int maxLength) {
        return abbreviate(value, maxLength, "");
    }

    /** 비어 있는 값은 기본값으로 대체하고, 길면 축약한다. */
    public static String abbreviate(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    /** 공백을 제거한 소문자 문자열로 정규화한다. */
    public static String normalizeCompactLower(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    /** 앞뒤 공백을 제거한 소문자 문자열로 정규화한다. */
    public static String normalizeTrimLower(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 텍스트에 주어진 키워드가 하나라도 포함되는지 확인한다. */
    public static boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
