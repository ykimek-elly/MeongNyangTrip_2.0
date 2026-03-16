package com.team.meongnyang.recommendation.log.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long dogId;

  private String modelName;

  @Column(columnDefinition = "TEXT")
  private String prompt;

  @Column(columnDefinition = "TEXT")
  private String recommendedPlaces;

  @Column(columnDefinition = "TEXT")
  private String ragContext;

  @Column(columnDefinition = "TEXT")
  private String responseText;

  private boolean fallbackUsed;

  private boolean cacheHit;

  private Long latencyMs;
}
