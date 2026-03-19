package com.team.meongnyang.recommendation.notification.dto;

import com.team.meongnyang.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 파이프라인 실행 후 알림 발송 단계로 전달할 결과 DTO.
 *
 * <p>알림 메시지 조합에 필요한 사용자/반려견/날씨/추천 장소/AI 코멘트 정보를 함께 담는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationNotificationResult {

  /** 사용자 식별자 */
  private Long userId;

  /** 반려견 식별자 */
  private Long petId;

  /** 반려견 이름 */
  private String petName;

  /** 날씨 유형 (SUNNY, RAINY, CLOUDY, HEATWAVE, COLD_WAVE 등) */
  private String weatherType;

  /** 산책 가능 수준 (GOOD, CAUTION, DANGEROUS 등) */
  private String weatherWalkLevel;

  /** 날씨 요약 문장 */
  private String weatherSummary;

  /** 최종 추천 장소 */
  private Place place;

  /** AI 추천 코멘트 또는 fallback 문장 */
  private String message;

  /** fallback 응답 사용 여부 */
  private boolean fallbackUsed;

  /** Gemini 캐시 사용 여부 */
  private boolean cacheHit;
}
