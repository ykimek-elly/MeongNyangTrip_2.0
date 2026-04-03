package com.team.meongnyang.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 추천 점수의 구성 요소를 나타내는 데이터 객체
 * 각 점수 섹션별 점수와 요약, 세부 항목 정보를 포함한다.
 */
@Getter
@Builder
public class ScoreBreakdown {

    private String section;
    private double score;
    private double maxScore;
    private String summary;
    private List<ScoreDetail> details;
}
