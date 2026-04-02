package com.team.meongnyang.recommendation.dto;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecommendationCachePayload {

  private Long userId;
  private Long petId;
  private String petName;
  private String dateKey;
  private String batchExecutionId;
  private PlaceCachePayload place;
  private String weatherType;
  private String weatherWalkLevel;
  private String weatherSummary;
  private String notificationMessage;
  private boolean fallbackUsed;
  private boolean geminiCacheHit;
  private String geminiCacheKey;
  private String geminiResponse;
  private LocalDateTime recommendedAt;

  public static DailyRecommendationCachePayload from(
          RecommendationNotificationResult result,
          String dateKey,
          String batchExecutionId,
          LocalDateTime recommendedAt
  ) {
    return DailyRecommendationCachePayload.builder()
            .userId(result.getUserId())
            .petId(result.getPetId())
            .petName(result.getPetName())
            .dateKey(dateKey)
            .batchExecutionId(batchExecutionId)
            .place(PlaceCachePayload.from(result.getPlace()))
            .weatherType(result.getWeatherType())
            .weatherWalkLevel(result.getWeatherWalkLevel())
            .weatherSummary(result.getWeatherSummary())
            .notificationMessage(result.getMessage())
            .fallbackUsed(result.isFallbackUsed())
            .geminiCacheHit(result.isCacheHit())
            .geminiCacheKey(result.getGeminiCacheKey())
            .geminiResponse(result.getAiResponse())
            .recommendedAt(recommendedAt)
            .build();
  }

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
            .message(notificationMessage)
            .fallbackUsed(fallbackUsed)
            .cacheHit(geminiCacheHit)
            .aiResponse(geminiResponse)
            .geminiCacheKey(geminiCacheKey)
            .build();
  }
}
