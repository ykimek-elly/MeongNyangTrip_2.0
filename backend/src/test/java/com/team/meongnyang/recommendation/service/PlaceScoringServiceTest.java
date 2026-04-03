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
    @DisplayName("理쒓렐 異붿쿇???μ냼???ㅼ뼇???섎꼸?곕줈 ?쒖쐞媛 ?대젮媛꾨떎")
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
                Map.of(1L, new AiLogService.RecommendationDiversityPenalty(25.0, "理쒓렐 1??異붿쿇"))
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
    @DisplayName("description怨?tags媛 鍮꾩뼱??overview? 釉붾줈洹??좏샇瑜?諛섏쁺?쒕떎")
    void scorePlaces_usesOverviewAndBlogSignalsWhenDescriptionAndTagsAreBlank() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

        Place enrichedPlace = Place.builder()
                .id(1L)
                .title("Signal Rich Place")
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .overview("諛섎젮寃ш낵 ?곗콉?섍린 醫뗪퀬 議곗슜??怨듭썝?쇰줈 ?볦? ?붾뵒? ?곗콉濡쒓? ???뺣퉬?섏뼱 ?덉뒿?덈떎.")
                .blogPositiveTags("?곗콉,議곗슜,?볦쓬")
                .aiRating(3.8)
                .blogCount(1200)
                .isVerified(true)
                .build();

        Place sparsePlace = Place.builder()
                .id(2L)
                .title("Sparse Place")
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .aiRating(2.2)
                .blogCount(1)
                .isVerified(true)
                .build();

        List<ScoredPlace> rankedPlaces = placeScoringService.scorePlaces(
                List.of(sparsePlace, enrichedPlace),
                user,
                null,
                null
        );

        assertThat(rankedPlaces).hasSize(2);
        assertThat(rankedPlaces.get(0).getPlace().getId()).isEqualTo(1L);
        assertThat(rankedPlaces.get(0).getEnvironmentFitScore()).isGreaterThan(rankedPlaces.get(1).getEnvironmentFitScore());
        assertThat(rankedPlaces.get(0).getBonusScore()).isGreaterThan(rankedPlaces.get(1).getBonusScore());
    }

    @Test
    @DisplayName("?꾨낫媛 ?곸쑝硫??ㅼ뼇???섎꼸?곕? ?꾪솕?쒕떎")
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
                Map.of(1L, new AiLogService.RecommendationDiversityPenalty(25.0, "理쒓렐 1??異붿쿇"))
        );

        ScoredPlace penalizedPlace = rankedPlaces.stream()
                .filter(scoredPlace -> scoredPlace.getPlace().getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(penalizedPlace.getPenaltyScore()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("?μ냼 硫뷀??곗씠?곌? ?띾????μ냼媛 ???믪? ?먯닔瑜?諛쏅뒗??)
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
        assertThat(rankedPlaces.get(0).getSectionScores()).containsKeys("諛섎젮?숇Ъ ?곹빀??, "?좎뵪 ?곹빀??);
    }

    @Test
    @DisplayName("媛쒖씤??議곌굔???ㅻⅤ硫?理쒖쥌 ?쒖쐞??諛붾먮떎")
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
                .personality("?덈??섍퀬 議곗슜??)
                .preferredPlace("?ㅻ궡移댄럹")
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
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("DINING")
                .description("議곗슜???ㅻ궡 移댄럹, 諛섎젮?숇Ъ ?숇컲 媛??)
                .tags("?ㅻ궡,移댄럹,議곗슜???ロ봽?뚮뱾由?)
                .chkPetInside("Y")
                .petPolicy("?ㅻ궡 ?숇컲 媛??)
                .petFacility("臾쇨렇由??댁떇怨듦컙")
                .rating(4.3)
                .reviewCount(20)
                .blogCount(10)
                .build();

        Place outdoorPark = Place.builder()
                .id(2L)
                .title("Outdoor Park")
                .address("?쒖슱")
                .latitude(37.28)
                .longitude(127.02)
                .category("PLACE")
                .description("?볦? 怨듭썝怨??곗콉濡쒓? ?덈뒗 ?쇱쇅 ?μ냼")
                .tags("?쇱쇅,怨듭썝,?곗콉濡??볦쓬")
                .chkPetInside("N")
                .petPolicy("?쇱쇅 ?숇컲 媛??)
                .petFacility("?곗콉濡?)
                .rating(4.8)
                .reviewCount(250)
                .blogCount(120)
                .blogPositiveTags("?볦쓬,?곗콉?섍린醫뗭쓬")
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
        assertThat(rankedPlaces.get(0).getAppliedBoosts()).isNotEmpty();
        assertThat(rankedPlaces.get(1).getAppliedPenalties()).isNotEmpty();
    }

    @Test
    @DisplayName("紐낆떆??諛섎젮?숇Ъ 遺덇? ?μ냼???먯닔 怨꾩궛 ??곸뿉???쒖쇅?쒕떎")
    void scorePlaces_filtersBlockedPlaces() {
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

        Place blockedPlace = Place.builder()
                .id(1L)
                .title("Blocked Hotel")
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("STAY")
                .description("諛섎젮?숇Ъ 異쒖엯??遺덇??ν븳 ?숈냼")
                .petPolicy("諛섎젮?숇Ъ 異쒖엯 遺덇?")
                .accomCountPet("0")
                .build();

        Place allowedPlace = Place.builder()
                .id(2L)
                .title("Allowed Hotel")
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("STAY")
                .description("???숇컲 ?숈냼")
                .chkPetInside("Y")
                .petPolicy("媛앹떎 ???숇컲 媛?? ?ъ쟾 臾몄쓽")
                .accomCountPet("2")
                .petFacility("???대찓?덊떚, ?앷린")
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

    @Test
    @DisplayName("자연/숲 선호는 공원과 산책로를 의미 매칭해 패널티를 주지 않는다")
    void scorePlaces_matchesNatureForestPreferenceSemantically() {
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

        Pet pet = Pet.builder()
                .petId(1L)
                .petName("mong")
                .preferredPlace("자연/숲")
                .petActivity(Pet.PetActivity.NORMAL)
                .build();

        Place parkPlace = Place.builder()
                .id(10L)
                .title("Forest Park")
                .address("Seoul")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .description("공원과 산책로가 이어진 녹지 공간")
                .tags("공원,녹지,산책로")
                .blogPositiveTags("자연공간,산책")
                .build();

        ScoredPlace scoredPlace = placeScoringService.scorePlaces(
                List.of(parkPlace),
                null,
                pet,
                null,
                37.27,
                127.01
        ).get(0);

        assertThat(scoredPlace.getBonusScore()).isGreaterThan(0.0);
        assertThat(scoredPlace.getAppliedPenalties()).noneMatch(penalty -> penalty.contains("선호 장소와 비매칭"));
    }

    @Test
    @DisplayName("운동장 선호는 광장과 넓은 야외 공간을 의미 매칭한다")
    void scorePlaces_matchesPlaygroundPreferenceSemantically() {
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(1.0);

        Pet pet = Pet.builder()
                .petId(2L)
                .petName("nyang")
                .preferredPlace("운동장")
                .petActivity(Pet.PetActivity.HIGH)
                .build();

        Place plazaPlace = Place.builder()
                .id(20L)
                .title("Open Plaza")
                .address("Seoul")
                .latitude(37.28)
                .longitude(127.02)
                .category("PLACE")
                .description("광장 중심의 넓은 야외 공간")
                .tags("광장,넓은 야외 공간")
                .blogPositiveTags("활동하기 좋음")
                .build();

        ScoredPlace scoredPlace = placeScoringService.scorePlaces(
                List.of(plazaPlace),
                null,
                pet,
                null,
                37.27,
                127.01
        ).get(0);

        assertThat(scoredPlace.getBonusScore()).isGreaterThan(0.0);
        assertThat(scoredPlace.getAppliedPenalties()).noneMatch(penalty -> penalty.contains("선호 장소와 비매칭"));
    }
    private Place detailedPlace(Long id, String title) {
        return Place.builder()
                .id(id)
                .title(title)
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .description("諛섎젮?숇Ъ怨?諛⑸Ц?섍린 醫뗭? ?ㅻ궡 移댄럹")
                .rating(4.8)
                .reviewCount(180)
                .tags("?ㅻ궡?숇컲媛??二쇱감,?곗콉濡??ロ봽?뚮뱾由?)
                .chkPetInside("Y")
                .aiRating(4.4)
                .blogCount(90)
                .blogPositiveTags("泥?껐,苡뚯쟻,議곗슜,移쒖젅")
                .petPolicy("?ㅻ궡 ?숇컲 媛?? 紐⑹쨪 李⑹슜, 諛곕? 泥섎━")
                .petFacility("臾쇨렇由? ???대찓?덊떚, ?곗콉濡?)
                .build();
    }

    private Place constrainedPlace(Long id, String title) {
        return Place.builder()
                .id(id)
                .title(title)
                .address("?쒖슱")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .description("?쇱쇅 ?꾩＜ ?μ냼")
                .rating(4.1)
                .reviewCount(3)
                .tags("?쇱쇅")
                .aiRating(2.8)
                .blogCount(1)
                .petPolicy("?ㅻ궡 遺덇?, ?ъ쟾 臾몄쓽")
                .blogNegativeTags("?쇱옟,?뚯쓬")
                .build();
    }
}

