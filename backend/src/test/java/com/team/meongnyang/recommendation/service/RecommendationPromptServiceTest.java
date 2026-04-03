package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPromptServiceTest {

    private final RecommendationPromptService service = new RecommendationPromptService();

    @Test
    @DisplayName("프롬프트에 설명 필수 근거와 금지 표현 규칙이 포함된다")
    void buildRecommendationPrompt_emphasizesEvidenceAndStyleRules() {
        RecommendationEvidenceContext context = RecommendationEvidenceContext.builder()
                .userProfileSection("사용자 정보")
                .petProfileSection("반려동물 정보")
                .weatherSection("날씨 정보")
                .recommendationDecisionSummary("추천 판단 요약")
                .explanationFocusSection("- 반드시 설명에 포함할 근거: 산책하기 편한 흐름")
                .topPlaceEvidenceSection("상위 장소 근거")
                .supplementalGuidelineSection("추가 지침")
                .contextSnapshot("스냅샷")
                .build();

        String prompt = service.buildRecommendationPrompt(context);

        assertThat(prompt).contains("설명 필수 근거");
        assertThat(prompt).contains("boost 근거를 최소 2개 이상");
        assertThat(prompt).contains("날씨 관련 근거를 최소 1개");
        assertThat(prompt).contains("반려동물 관련 boost");
        assertThat(prompt).contains("좋습니다");
        assertThat(prompt).contains("야외 공간에서 산책하기 좋습니다");
    }
}
