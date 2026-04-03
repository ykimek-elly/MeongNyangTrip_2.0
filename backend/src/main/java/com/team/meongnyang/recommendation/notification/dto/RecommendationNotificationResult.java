package com.team.meongnyang.recommendation.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team.meongnyang.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 파이프라인과 알림 단계에서 함께 사용하는 결과 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationNotificationResult {

  private Long userId;
  private Long petId;
  private String petName;
  private String weatherType;
  private String weatherWalkLevel;
  private String weatherSummary;
  private Place place;
  private String message;
  private String recommendationDescription;
  private boolean fallbackUsed;
  private boolean cacheHit;
  private boolean error;
  private String errorCode;

  /** 배치/조회 캐시 저장용 원본 AI 응답 */
  @JsonIgnore
  private String aiResponse;

  /** 배치/조회 캐시 저장용 Gemini 캐시 키 */
  @JsonIgnore
  private String geminiCacheKey;
}
