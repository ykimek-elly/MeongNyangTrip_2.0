package com.team.meongnyang.recommendation.util;

public final class RecommendationNumberUtils {

    private RecommendationNumberUtils() {
    }

    /** 소수 첫째 자리까지 반올림한다. */
    public static double roundOneDecimal(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
