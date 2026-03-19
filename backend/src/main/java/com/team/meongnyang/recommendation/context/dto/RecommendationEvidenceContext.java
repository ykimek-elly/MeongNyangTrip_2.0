package com.team.meongnyang.recommendation.context.dto;

import lombok.Builder;
import lombok.Getter;

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
