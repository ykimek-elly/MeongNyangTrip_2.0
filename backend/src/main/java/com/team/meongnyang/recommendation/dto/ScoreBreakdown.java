package com.team.meongnyang.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScoreBreakdown {

    private String section;
    private double score;
    private double maxScore;
    private String summary;
    private List<ScoreDetail> details;
}
