package com.team.meongnyang.recommendation.context.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 추천 결과 생성을 위한 근거 데이터를 구성하는 컨텍스트 객체
 * 사용자, 반려동물, 날씨, 장소 정보 등을 종합하여 AI가 추천 이유를 생성할 수 있도록 제공한다.
 */
@Getter
@Builder
public class RecommendationEvidenceContext {

    private final String userProfileSection;
    private final String petProfileSection;
    private final String weatherSection;
    private final String recommendationDecisionSummary;
    private final String topPlaceEvidenceSection;
    private final String supplementalGuidelineSection;
    private final String contextSnapshot;
}
