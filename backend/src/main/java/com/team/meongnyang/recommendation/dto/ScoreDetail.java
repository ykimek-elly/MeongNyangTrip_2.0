package com.team.meongnyang.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreDetail {

    private String section;
    private String item;
    private double score;
    private double maxScore;
    private String reason;
}
