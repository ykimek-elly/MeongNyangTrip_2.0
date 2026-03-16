package com.team.meongnyang.orchestrator.dto;

import com.team.meongnyang.place.entity.Place;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ScoredPlace {

    private Place place;
    private double totalScore;

    private double dogFitScore;
    private double weatherScore;
    private double placeEnvScore;
    private double distanceScore;
    /**
     * Legacy field name kept for compatibility.
     * Internally this now represents the "부가 요소" score.
     */
    private double historyScore;

    private Map<String, Double> sectionScores;
    private List<ScoreDetail> scoreDetails;
    private String reason;
}
