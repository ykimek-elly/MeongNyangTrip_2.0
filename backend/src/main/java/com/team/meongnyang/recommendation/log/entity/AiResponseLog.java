package com.team.meongnyang.recommendation.log.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
