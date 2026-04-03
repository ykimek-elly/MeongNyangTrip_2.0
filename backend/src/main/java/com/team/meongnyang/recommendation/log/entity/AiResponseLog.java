package com.team.meongnyang.recommendation.log.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * AI 추천 요청 및 응답 결과를 기록하는 로그 엔티티
 * 프롬프트, 추천 결과, 응답 내용, 성능 정보 등을 저장하여 추적 및 분석에 활용된다.
 */
@Entity
@Table(name = "ai_response_logs")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long petId;

  private String batchExecutionId;

  private String modelName;

  @Column(columnDefinition = "TEXT")
  private String prompt;

  @Column(columnDefinition = "TEXT")
  private String recommendedPlaces;

  private Long recommendedPlaceId;

  @Column(columnDefinition = "TEXT")
  private String ragContext;

  @Column(columnDefinition = "TEXT")
  private String responseText;

  private boolean fallbackUsed;

  private boolean cacheHit;

  private Long latencyMs;
}
