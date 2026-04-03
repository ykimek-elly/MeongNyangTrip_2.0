package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPromptServiceTest {

    private final RecommendationPromptService service = new RecommendationPromptService();

    @Test
    @DisplayName("프롬프트에 category 우선 해석 규칙과 category별 설명 기준이 포함된다")
    void buildRecommendationPrompt_includesCategoryFirstRules() {
        RecommendationEvidenceContext context = RecommendationEvidenceContext.builder()
                .userProfileSection("사용자 정보")
                .petProfileSection("반려동물 정보")
                .weatherSection("날씨 정보")
                .recommendationDecisionSummary("추천 판단 요약")
                .explanationFocusSection("설명 필수 근거")
                .topPlaceEvidenceSection("상위 장소 근거")
                .supplementalGuidelineSection("추가 지침")
                .contextSnapshot("스냅샷")
                .build();

        String prompt = service.buildRecommendationPrompt(context);

        assertThat(prompt).contains("장소의 category를 먼저 해석한다");
        assertThat(prompt).contains("overview, 태그, 부가 정보는 반드시 category의 틀 안에서 해석한다");
        assertThat(prompt).contains("DINING은 산책 장소처럼 설명하지 않는다");
        assertThat(prompt).contains("DINING + 테라스 + 수변공원 뷰");
        assertThat(prompt).contains("STAY: 숙박, 머무름, 쉬는 흐름");
        assertThat(prompt).contains("DINING: 카페/식음/브런치");
        assertThat(prompt).contains("PLACE: 산책, 둘러보기, 이동 흐름");
        assertThat(prompt).contains("알림요약 category 어휘 가이드");
    }
}
