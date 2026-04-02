package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.user.entity.Pet;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gemini 응답 재사용을 위한 공용 컨텍스트 fingerprint를 만든다.
 *
 * <p>정확한 사용자 식별값은 제외하고, 날씨 band와 반려동물 그룹, 상위 추천 장소를 사용한다.
 */
@Service
public class GeminiContextFingerprintService {

    /**
     * Gemini 컨텍스트 fingerprint를 생성한다.
     *
     * @param weatherContext 날씨 컨텍스트
     * @param pet 대표 반려동물
     * @param rankedPlaces 상위 추천 장소 목록
     * @return 정규화된 fingerprint 문자열
     */
    public String buildFingerprint(WeatherContext weatherContext, Pet pet, List<ScoredPlace> rankedPlaces) {
        String weatherPart = weatherContext == null
                ? "UNKNOWN"
                : "%s:%s:%s".formatted(
                        normalize(weatherContext.getWalkLevel()),
                        normalize(weatherContext.getPrecipitationType()),
                        temperatureBand(weatherContext.getTemperature())
                );

        String petPart = pet == null
                ? "UNKNOWN"
                : "%s:%s:%s".formatted(
                        pet.getPetSize() == null ? "UNKNOWN" : pet.getPetSize().name(),
                        pet.getPetActivity() == null ? "UNKNOWN" : pet.getPetActivity().name(),
                        normalize(pet.getPreferredPlace())
                );

        String placePart = rankedPlaces == null || rankedPlaces.isEmpty()
                ? "NONE"
                : rankedPlaces.stream()
                .limit(3)
                .map(scoredPlace -> String.valueOf(scoredPlace.getPlace().getId()))
                .collect(Collectors.joining(","));

        return "v1|" + weatherPart + "|" + petPart + "|" + placePart;
    }

    private String normalize(String value) {
        return value == null ? "UNKNOWN" : value.trim().toUpperCase();
    }

    private String temperatureBand(double temperature) {
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
}
