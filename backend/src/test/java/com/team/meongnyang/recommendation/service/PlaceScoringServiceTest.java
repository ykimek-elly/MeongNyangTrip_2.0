package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PlaceScoringServiceTest {

    @Mock
    private DistanceCalculator distanceCalculator;

    @InjectMocks
    private PlaceScoringService placeScoringService;

    @Test
    @DisplayName("최근 추천된 장소는 다양성 페널티로 순위가 내려간다")
    void scorePlaces_appliesDiversityPenalty() {
        Place penalizedPlace = detailedPlace(1L, "Alpha Cafe");
        Place normalPlace = detailedPlace(2L, "Beta Cafe");

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(
                        penalizedPlace,
                        normalPlace,
                        detailedPlace(3L, "Gamma Cafe"),
                        detailedPlace(4L, "Delta Cafe")
                ),
                null,
                null,
                null,
                37.27,
                127.01,
                Map.of(1L, new AiLogService.RecommendationDiversityPenalty(25.0, "최근 1회 추천"))
        );

        assertThat(rankedPlaces).hasSize(4);
        assertThat(rankedPlaces.get(0).getPlace().getId()).isNotEqualTo(1L);
        assertThat(rankedPlaces.stream()
                .filter(scoredPlace -> scoredPlace.getPlace().getId().equals(1L))
                .findFirst()
                .orElseThrow()
                .getPenaltyScore()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("후보가 적으면 다양성 페널티를 완화한다")
    void scorePlaces_relaxesDiversityPenaltyWhenCandidatesAreFew() {
        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(
                        detailedPlace(1L, "Alpha Cafe"),
                        detailedPlace(2L, "Beta Cafe"),
                        detailedPlace(3L, "Gamma Cafe")
                ),
                null,
                null,
                null,
                37.27,
                127.01,
                Map.of(1L, new AiLogService.RecommendationDiversityPenalty(25.0, "최근 1회 추천"))
        );

        ScoredPlace penalizedPlace = rankedPlaces.stream()
                .filter(scoredPlace -> scoredPlace.getPlace().getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(penalizedPlace.getPenaltyScore()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("Place 엔티티 신호가 풍부한 장소가 더 높은 점수를 받는다")
    void scorePlaces_scoresUsingPlaceEntitySignals() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        Place strongPlace = detailedPlace(1L, "Alpha Cafe");
        Place weakPlace = constrainedPlace(2L, "Beta Cafe");

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(weakPlace, strongPlace),
                user,
                null,
                null
        );

        assertThat(rankedPlaces).hasSize(2);
        assertThat(rankedPlaces.get(0).getPlace().getId()).isEqualTo(1L);
        assertThat(rankedPlaces.get(0).getTotalScore()).isGreaterThan(rankedPlaces.get(1).getTotalScore());
        assertThat(rankedPlaces.get(0).getSectionScores()).containsKeys("펫 출입/정책 적합도", "장소 품질/신뢰도");
    }

    @Test
    @DisplayName("명시적 반려동물 불가 장소는 점수 계산 대상에서 제외한다")
    void scorePlaces_filtersBlockedPlaces() {
        Place blockedPlace = Place.builder()
                .id(1L)
                .title("Blocked Hotel")
                .address("서울")
                .latitude(37.27)
                .longitude(127.01)
                .category("STAY")
                .description("반려동물 출입이 불가능한 숙소")
                .petPolicy("반려동물 출입 불가")
                .accomCountPet("0")
                .build();

        Place allowedPlace = Place.builder()
                .id(2L)
                .title("Allowed Hotel")
                .address("서울")
                .latitude(37.27)
                .longitude(127.01)
                .category("STAY")
                .description("펫 동반 숙소")
                .chkPetInside("Y")
                .petPolicy("객실 내 동반 가능, 사전 문의")
                .accomCountPet("2")
                .petFacility("펫 어메니티, 식기")
                .rating(4.5)
                .reviewCount(35)
                .blogCount(20)
                .build();

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(blockedPlace, allowedPlace),
                null,
                null,
                null,
                37.27,
                127.01
        );

        assertThat(rankedPlaces).hasSize(1);
        assertThat(rankedPlaces.get(0).getPlace().getId()).isEqualTo(2L);
    }

    private Place detailedPlace(Long id, String title) {
        return Place.builder()
                .id(id)
                .title(title)
                .address("서울")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .description("반려동물과 방문하기 좋은 실내 카페")
                .rating(4.8)
                .reviewCount(180)
                .tags("실내동반가능,주차,산책로,펫프렌들리")
                .chkPetInside("Y")
                .aiRating(4.4)
                .blogCount(90)
                .blogPositiveTags("청결,쾌적,조용,친절")
                .petPolicy("실내 동반 가능, 목줄 착용, 배변 처리")
                .petFacility("물그릇, 펫 어메니티, 산책로")
                .build();
    }

    private Place constrainedPlace(Long id, String title) {
        return Place.builder()
                .id(id)
                .title(title)
                .address("서울")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .description("야외 위주 장소")
                .rating(4.1)
                .reviewCount(3)
                .tags("야외")
                .aiRating(2.8)
                .blogCount(1)
                .petPolicy("실내 불가, 사전 문의")
                .blogNegativeTags("혼잡,소음")
                .build();
    }
}
