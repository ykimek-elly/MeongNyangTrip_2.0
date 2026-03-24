package com.team.meongnyang.recommendation.notification.template;

import java.util.Locale;

public enum KakaoWeatherTemplateType {
    SUNNY,
    CLOUDY,
    RAIN,
    HOT,
    COLD;

    public static KakaoWeatherTemplateType from(String weatherType) {
        if (weatherType == null || weatherType.isBlank()) {
            return SUNNY;
        }

        return switch (weatherType.trim().toUpperCase(Locale.ROOT)) {
            case "RAIN", "RAINY", "MONSOON" -> RAIN;
            case "CLOUDY" -> CLOUDY;
            case "HOT", "HEATWAVE" -> HOT;
            case "COLD", "COLD_WAVE" -> COLD;
            case "GOOD", "SUNNY", "CLEAR" -> SUNNY;
            default -> SUNNY;
        };
    }
    public static final String SUNNY_TEMPLATE = """
            [멍냥트립]
            고객님께서 설정하신 반려동물 맞춤 알림에 따라,
            #{petName}의 오늘 반려생활을 안내드립니다.

            ☀️ 오늘 날씨 : 맑음
            야외 활동에 적합한 날씨로 산책을 권장드립니다.

            📍 추천 장소 : #{placeName}
            💬 #{comment}

            ※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,
            등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.
            """;

    public static final String CLOUDY_TEMPLATE = """
            [멍냥트립]
            고객님께서 설정하신 반려동물 맞춤 알림에 따라,
            #{petName}의 오늘 반려생활을 안내드립니다.

            🌥 오늘 날씨 : 흐림
            강한 햇빛이 없어 #{petName}이 비교적 편안하게 활동할 수 있습니다.

            📍 추천 장소 : #{placeName}
            💬 #{comment}

            ※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,
            등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.
            """;

    public static final String RAIN_TEMPLATE = """
            [멍냥트립]
            고객님께서 설정하신 반려동물 맞춤 알림에 따라,
            #{petName}의 오늘 반려생활을 안내드립니다.

            ☔ 오늘 날씨 : 장마
            외부 활동이 어려운 날씨로 실내 활동을 권장드립니다.

            📍 추천 장소 : #{placeName}
            💬 #{comment}

            ※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,
            등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.
            """;

    public static final String HOT_TEMPLATE = """
            [멍냥트립]
            고객님께서 설정하신 반려동물 맞춤 알림에 따라,
            #{petName}의 오늘 반려생활을 안내드립니다.

            🌡 오늘 날씨 : 폭염
            고온의 지면은 #{petName}에게 위험할 수 있어 실내 활동을 권장드립니다.

            📍 추천 장소 : #{placeName}
            💬 #{comment}

            ※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,
            등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.
            """;

    public static final String COLD_TEMPLATE = """
            [멍냥트립]
            고객님께서 설정하신 반려동물 맞춤 알림에 따라,
            #{petName}의 오늘 반려생활을 안내드립니다.

            🧊 오늘 날씨 : 한파
            #{petName}이 추위에 노출되지 않도록 실내 활동을 권장드립니다.

            📍 추천 장소 : #{placeName}
            💬 #{comment}

            ※ 본 알림은 고객님이 신청한 날씨 기반 반려동물 케어 알림으로,
            등록하신 반려동물 정보 및 날씨 조건에 따라 반복 발송될 수 있습니다.
            """;
}
