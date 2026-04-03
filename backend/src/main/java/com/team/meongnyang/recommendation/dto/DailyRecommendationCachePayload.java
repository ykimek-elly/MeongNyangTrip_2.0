package com.team.meongnyang.recommendation.dto;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 일일 추천 결과를 캐시에 저장하기 위한 데이터 객체
 * 추천 결과, 날씨 정보, AI 응답 등을 포함하여 재사용 및 중복 전송 방지에 활용된다.
 */
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
  private String recommendationDescription;
  private boolean fallbackUsed;
  private boolean geminiCacheHit;
  private String geminiCacheKey;
  private String geminiResponse;
  private LocalDateTime recommendedAt;

  /**
   * 추천 결과를 캐시 저장용 데이터로 변환한다.
   */
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
            .recommendationDescription(result.getRecommendationDescription())
            .fallbackUsed(result.isFallbackUsed())
            .geminiCacheHit(result.isCacheHit())
            .geminiCacheKey(result.getGeminiCacheKey())
            .geminiResponse(result.getAiResponse())
            .recommendedAt(recommendedAt)
            .build();
  }


  /**
   * 캐시 데이터를 추천 알림 결과 객체로 복원한다.
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
            .message(notificationMessage)
            .recommendationDescription(recommendationDescription)
            .fallbackUsed(fallbackUsed)
            .cacheHit(geminiCacheHit)
            .aiResponse(geminiResponse)
            .geminiCacheKey(geminiCacheKey)
            .build();
  }
}
