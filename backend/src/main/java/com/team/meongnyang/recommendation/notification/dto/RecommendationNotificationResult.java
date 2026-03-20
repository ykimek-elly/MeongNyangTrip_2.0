package com.team.meongnyang.recommendation.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team.meongnyang.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 파이프라인 실행 후 알림 단계로 전달되는 결과 DTO.
 *
 * <p>알림 메시지 조합과 웹 조회 재사용에 필요한 사용자/날씨/장소/AI 응답 요약 정보를 담는다.
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
  private boolean fallbackUsed;
  private boolean cacheHit;

  /** 배치 일자 캐시 저장용 원본 AI 응답 */
  @JsonIgnore
  private String aiResponse;

  /** 배치 일자 캐시 저장용 Gemini 캐시 키 */
  @JsonIgnore
  private String geminiCacheKey;
}
