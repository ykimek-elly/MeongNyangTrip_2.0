package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceScoringServiceTest {

    @Mock
    private DistanceCalculator distanceCalculator;

    @InjectMocks
    private PlaceScoringService placeScoringService;

    @Test
    @DisplayName("최근 추천된 장소는 다양성 패널티로 순위가 뒤로 밀린다")
    void scorePlaces_appliesDiversityPenalty() {
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

        Place penalizedPlace = fixturePlace(1L, "Alpha Cafe");
        Place normalPlace = fixturePlace(2L, "Beta Cafe");

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(
                        penalizedPlace,
                        normalPlace,
                        fixturePlace(3L, "Gamma Cafe"),
                        fixturePlace(4L, "Delta Cafe")
                ),
                null,
                null,
                null,
                37.27,
                127.01,
                Map.of(1L, new AiLogService.RecommendationDiversityPenalty(25.0, "최근 1일 내 추천"))
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
    @DisplayName("후보가 3개 이하면 다양성 패널티를 완화한다")
    void scorePlaces_relaxesDiversityPenaltyWhenCandidatesAreFew() {
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(
                        fixturePlace(1L, "Alpha Cafe"),
                        fixturePlace(2L, "Beta Cafe"),
                        fixturePlace(3L, "Gamma Cafe")
                ),
                null,
                null,
                null,
                37.27,
                127.01,
                Map.of(1L, new AiLogService.RecommendationDiversityPenalty(25.0, "최근 1일 내 추천"))
        );

        ScoredPlace penalizedPlace = rankedPlaces.stream()
                .filter(scoredPlace -> scoredPlace.getPlace().getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(penalizedPlace.getPenaltyScore()).isEqualTo(12.5);
    }

    private Place fixturePlace(Long id, String title) {
        return Place.builder()
                .id(id)
                .title(title)
                .address("경기도 수원시")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .description("반려동물과 함께 방문하기 좋은 실내 카페")
                .build();
    }
}
