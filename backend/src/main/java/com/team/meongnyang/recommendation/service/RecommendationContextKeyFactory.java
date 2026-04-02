package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 추천 결과 캐시용 컨텍스트 키를 생성한다.
 *
 * <p>사용자 고유 ID는 포함하되, 날씨와 반려동물 정보는 band/fingerprint로 정규화해서
 * 같은 의미의 요청이 같은 키로 묶이도록 한다.
 */
@Component
public class RecommendationContextKeyFactory {

    /**
     * 추천 결과 캐시 키를 생성한다.
     *
     * @param user 사용자 정보
     * @param pet 대표 반려동물
     * @param weatherContext 정규화된 날씨
     * @param candidates 후보 장소 목록
     * @param lastRecommendedPlaceId 최근 추천 장소 ID
     * @return recommendation result Redis 키
     */
    public String buildResultKey(
            User user,
            Pet pet,
            WeatherContext weatherContext,
            List<Place> candidates,
            Long lastRecommendedPlaceId
    ) {
        String raw = String.join("|",
                "v1",
                "user:" + user.getUserId(),
                "pet:" + petFingerprint(pet),
                "weather:" + weatherFingerprint(weatherContext),
                "candidates:" + candidateFingerprint(candidates),
                "recent:" + (lastRecommendedPlaceId == null ? "none" : lastRecommendedPlaceId)
        );
        return "recommendation:result:" + user.getUserId() + ":" + sha256(raw);
    }

    private String petFingerprint(Pet pet) {
        return String.join(":",
                pet.getPetSize() == null ? "UNKNOWN" : pet.getPetSize().name(),
                pet.getPetActivity() == null ? "UNKNOWN" : pet.getPetActivity().name(),
                pet.getPreferredPlace() == null ? "UNKNOWN" : pet.getPreferredPlace().trim().toUpperCase()
        );
    }

    private String weatherFingerprint(WeatherContext weatherContext) {
        if (weatherContext == null) {
            return "UNKNOWN";
        }
        return String.join(":",
                bandOfTemperature(weatherContext.getTemperature()),
                bandOfHumidity(weatherContext.getHumidity()),
                bandOfWind(weatherContext.getWindSpeed()),
                normalize(weatherContext.getPrecipitationType()),
                normalize(weatherContext.getWalkLevel())
        );
    }

    private String candidateFingerprint(List<Place> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "empty";
        }
        return candidates.stream()
                .limit(10)
                .map(place -> place.getId() == null ? place.getContentId() : String.valueOf(place.getId()))
                .collect(Collectors.joining(","));
    }

    private String bandOfTemperature(double temperature) {
        if (temperature < 0) {
            return "LT0";
        }
        if (temperature < 10) {
            return "0_9";
        }
        if (temperature < 20) {
            return "10_19";
        }
        if (temperature < 28) {
            return "20_27";
        }
        return "28_PLUS";
    }

    private String bandOfHumidity(int humidity) {
        if (humidity < 40) {
            return "DRY";
        }
        if (humidity < 70) {
            return "NORMAL";
        }
        return "HUMID";
    }

    private String bandOfWind(double windSpeed) {
        if (windSpeed < 2) {
            return "LOW";
        }
        if (windSpeed < 5) {
            return "MID";
        }
        return "HIGH";
    }

    private String normalize(String value) {
        return value == null ? "UNKNOWN" : value.trim().toUpperCase();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("추천 컨텍스트 해시 생성에 실패했습니다.", e);
        }
    }
}
