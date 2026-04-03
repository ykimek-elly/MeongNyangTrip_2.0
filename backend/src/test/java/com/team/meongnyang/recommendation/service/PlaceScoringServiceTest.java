package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.dto.ScoredPlace;
import com.team.meongnyang.recommendation.log.service.AiLogService;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

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
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

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
    @DisplayName("장소 메타데이터가 풍부한 장소가 더 높은 점수를 받는다")
    void scorePlaces_scoresUsingPlaceSignals() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

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
        assertThat(rankedPlaces.get(0).getSectionScores()).containsKeys("반려동물 적합도", "날씨 적합도");
    }

    @Test
    @DisplayName("개인화 조건이 다르면 최종 순위도 바뀐다")
    void scorePlaces_changesRankingByPetAndWeatherContext() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        Pet pet = Pet.builder()
                .petId(10L)
                .user(user)
                .petName("mong")
                .petBreed("maltese")
                .petSize(Pet.PetSize.SMALL)
                .petAge(12)
                .petWeight(new BigDecimal("4.50"))
                .petActivity(Pet.PetActivity.LOW)
                .personality("예민하고 조용함")
                .preferredPlace("실내카페")
                .isRepresentative(true)
                .build();

        WeatherContext dangerousWeather = WeatherContext.builder()
                .temperature(31.0)
                .humidity(80)
                .precipitationType("RAIN")
                .rainfall(3.0)
                .windSpeed(6.0)
                .raining(true)
                .cold(false)
                .hot(true)
                .windy(true)
                .walkLevel("DANGEROUS")
                .build();

        Place indoorCafe = Place.builder()
                .id(1L)
                .title("Indoor Cafe")
                .address("서울")
                .latitude(37.27)
                .longitude(127.01)
                .category("DINING")
                .description("조용한 실내 카페, 반려동물 동반 가능")
                .tags("실내,카페,조용함,펫프렌들리")
                .chkPetInside("Y")
                .petPolicy("실내 동반 가능")
                .petFacility("물그릇,휴식공간")
                .rating(4.3)
                .reviewCount(20)
                .blogCount(10)
                .build();

        Place outdoorPark = Place.builder()
                .id(2L)
                .title("Outdoor Park")
                .address("서울")
                .latitude(37.28)
                .longitude(127.02)
                .category("PLACE")
                .description("넓은 공원과 산책로가 있는 야외 장소")
                .tags("야외,공원,산책로,넓음")
                .chkPetInside("N")
                .petPolicy("야외 동반 가능")
                .petFacility("산책로")
                .rating(4.8)
                .reviewCount(250)
                .blogCount(120)
                .blogPositiveTags("넓음,산책하기좋음")
                .build();

        when(distanceCalculator.calculateDistanceKm(
                eq(user.getLatitude()),
                eq(user.getLongitude()),
                eq(indoorCafe.getLatitude()),
                eq(indoorCafe.getLongitude())
        )).thenReturn(1.2);
        when(distanceCalculator.calculateDistanceKm(
                eq(user.getLatitude()),
                eq(user.getLongitude()),
                eq(outdoorPark.getLatitude()),
                eq(outdoorPark.getLongitude())
        )).thenReturn(1.0);

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(outdoorPark, indoorCafe),
                user,
                pet,
                dangerousWeather
        );

        assertThat(rankedPlaces).hasSize(2);
        assertThat(rankedPlaces.get(0).getPlace().getId()).isEqualTo(1L);
        assertThat(rankedPlaces.get(0).getWeatherFitScore()).isGreaterThan(rankedPlaces.get(1).getWeatherFitScore());
    }

    @Test
    @DisplayName("명시적 반려동물 불가 장소는 점수 계산 대상에서 제외한다")
    void scorePlaces_filtersBlockedPlaces() {
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

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
