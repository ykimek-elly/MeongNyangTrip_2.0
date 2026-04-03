package com.team.meongnyang.recommendation.dto;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 결과를 캐시에 저장하기 위한 데이터 객체
 * 알림 전송 여부와 관계없이 추천 결과 자체를 재사용하기 위해 사용된다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResultCachePayload {

    private Long userId;
    private Long petId;
    private String petName;
    private String weatherType;
    private String weatherWalkLevel;
    private String weatherSummary;
    private PlaceCachePayload place;
    private String message;
    private boolean fallbackUsed;
    private boolean cacheHit;
    private boolean error;
    private String errorCode;
    private String aiResponse;
    private String geminiCacheKey;

    /**
     * 추천 결과 객체를 캐시용 데이터로 변환한다.
     */
    public static RecommendationResultCachePayload from(RecommendationNotificationResult result) {
        if (result == null) {
            return null;
        }

        return RecommendationResultCachePayload.builder()
                .userId(result.getUserId())
                .petId(result.getPetId())
                .petName(result.getPetName())
                .weatherType(result.getWeatherType())
                .weatherWalkLevel(result.getWeatherWalkLevel())
                .weatherSummary(result.getWeatherSummary())
                .place(PlaceCachePayload.from(result.getPlace()))
                .message(result.getMessage())
                .fallbackUsed(result.isFallbackUsed())
                .cacheHit(result.isCacheHit())
                .error(result.isError())
                .errorCode(result.getErrorCode())
                .aiResponse(result.getAiResponse())
                .geminiCacheKey(result.getGeminiCacheKey())
                .build();
    }

    /**
     * 캐시 데이터를 추천 결과 객체로 복원한다.
     */
    public RecommendationNotificationResult toNotificationResult() {
        Place restoredPlace = place == null ? null : place.toPlace();

        return RecommendationNotificationResult.builder()
                .userId(userId)
                .petId(petId)
                .petName(petName)
                .weatherType(weatherType)
                .weatherWalkLevel(weatherWalkLevel)
                .weatherSummary(weatherSummary)
                .place(restoredPlace)
                .message(message)
                .fallbackUsed(fallbackUsed)
                .cacheHit(cacheHit)
                .error(error)
                .errorCode(errorCode)
                .aiResponse(aiResponse)
                .geminiCacheKey(geminiCacheKey)
                .build();
    }
}
