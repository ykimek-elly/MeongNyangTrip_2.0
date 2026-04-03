package com.team.meongnyang.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 추천 점수의 세부 항목을 나타내는 데이터 객체
 * 각 항목별 점수와 해당 점수에 대한 이유를 포함한다.
 */
@Getter
@Builder
public class ScoreDetail {

    private String section;
    private String item;
    private double score;
    private double maxScore;
    private String reason;
}
