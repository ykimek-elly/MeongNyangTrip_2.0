package com.team.meongnyang.recommendation.context.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.context.dto.RecommendationEvidenceContext;
import com.team.meongnyang.recommendation.dto.ScoreDetail;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEvidenceContextServiceTest {

    private final RecommendationEvidenceContextService service = new RecommendationEvidenceContextService();

    @Test
    @DisplayName("설명 필수 근거에 날씨와 반려동물 boost가 자연어로 포함된다")
    void buildContext_includesNaturalizedBoostEvidence() {
        User user = User.builder()
                .nickname("tester")
                .notificationEnabled(true)
                .build();
        Pet pet = Pet.builder()
                .petName("Mong")
                .petBreed("비숑")
                .petAge(3)
                .petActivity(Pet.PetActivity.NORMAL)
                .personality("차분함")
                .preferredPlace("카페")
                .build();
        WeatherContext weather = WeatherContext.builder()
                .walkLevel("GOOD")
                .temperature(18.0)
                .humidity(45)
                .precipitationType("NONE")
                .rainfall(0.0)
                .windSpeed(1.5)
                .build();
        Place topPlace = Place.builder()
                .title("숲길 공원")
                .category("PLACE")
                .isVerified(true)
                .rating(4.8)
                .reviewCount(120)
                .build();

        ScoredPlace scoredPlace = ScoredPlace.builder()
                .place(topPlace)
                .totalScore(95.0)
                .personalFitScore(28.0)
                .weatherFitScore(17.0)
                .environmentFitScore(14.0)
                .mobilityFitScore(8.0)
                .bonusScore(5.0)
                .penaltyScore(1.0)
                .scoreDetails(List.of(
                        detail("날씨 적합도", "산책등급", 8.0, 8.0, "walkLevel과 장소 실내/실외 성격을 반영했습니다."),
                        detail("날씨 적합도", "바람", 3.0, 3.0, "강풍 여부에 따른 체감 환경을 반영했습니다."),
                        detail("반려동물 적합도", "품종", 6.0, 6.0, "품종 특성과 장소 유형의 일반적 궁합을 약하게 반영했습니다."),
                        detail("장소 환경 적합도", "동반 친화성", 6.0, 6.0, "반려동물 동반 관련 태그와 설명을 반영했습니다.")
                ))
                .appliedPenalties(List.of("선호 장소와 비매치-1.0"))
                .summary("요약")
                .reason("이유")
                .build();

        RecommendationEvidenceContext context = service.buildContext(user, pet, weather, List.of(scoredPlace));

        assertThat(context.getExplanationFocusSection()).contains("반드시 설명에 포함할 근거");
        assertThat(context.getExplanationFocusSection()).contains("산책하기 편한 흐름");
        assertThat(context.getExplanationFocusSection()).contains("비숑");
        assertThat(context.getExplanationFocusSection()).contains("선호 장소와 완전히 같지는 않지만");
    }

    private ScoreDetail detail(String section, String item, double score, double maxScore, String reason) {
        return ScoreDetail.builder()
                .section(section)
                .item(item)
                .score(score)
                .maxScore(maxScore)
                .reason(reason)
                .build();
    }
}
