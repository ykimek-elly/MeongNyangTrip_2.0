package com.team.meongnyang.recommendation.dto;

import com.team.meongnyang.place.entity.Place;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 점수 계산이 완료된 장소 정보를 담는 객체.
 * 장소와 섹션별 점수, 총점, 설명용 상세 근거를 함께 보관한다.
 */
@Getter
@Builder
public class ScoredPlace {

    private Place place;
    private double totalScore;

    private double personalFitScore;
    private double weatherFitScore;
    private double environmentFitScore;
    private double mobilityFitScore;
    private double bonusScore;
    private double penaltyScore;

    private Map<String, Double> sectionScores;
    private List<ScoreBreakdown> breakdowns;
    private List<ScoreDetail> scoreDetails;
    private String summary;
    private String reason;
}
