package com.team.meongnyang.recommendation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationAiResponseParserTest {

    private final RecommendationAiResponseParser parser = new RecommendationAiResponseParser();

    @Test
    @DisplayName("AI 응답에서 알림 요약과 추천 설명을 분리한다")
    void extractsSummaryAndDescription() {
        String aiResponse = """
                [추천설명]
                오늘은 바람이 약하고 기온이 안정적이라 산책 리듬을 유지하기 좋습니다.
                그중 호수공원은 잔디와 산책로가 함께 있어 활동량을 조절하기 수월합니다.

                [알림요약]
                호수공원, 선선한 날씨에 걷기 리듬을 맞추기 좋습니다.
                """;

        assertThat(parser.extractRecommendationDescription(aiResponse))
                .isEqualTo("""
                        오늘은 바람이 약하고 기온이 안정적이라 산책 리듬을 유지하기 좋습니다.
                        그중 호수공원은 잔디와 산책로가 함께 있어 활동량을 조절하기 수월합니다.
                        """.trim());
        assertThat(parser.extractNotificationSummary(aiResponse))
                .isEqualTo("호수공원, 선선한 날씨에 걷기 리듬을 맞추기 좋습니다.");
    }

    @Test
    @DisplayName("알림 요약 섹션이 없으면 설명은 원문 전체를 사용한다")
    void usesWholeResponseAsDescriptionWhenSummaryMissing() {
        String aiResponse = """
                [추천설명]
                실내 동선이 안정적이라 우천 시에도 이동 부담이 적습니다.
                """;

        assertThat(parser.extractNotificationSummary(aiResponse)).isNull();
        assertThat(parser.extractRecommendationDescription(aiResponse))
                .isEqualTo("실내 동선이 안정적이라 우천 시에도 이동 부담이 적습니다.");
    }
}
