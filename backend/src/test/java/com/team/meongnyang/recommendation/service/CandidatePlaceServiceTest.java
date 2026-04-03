package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatePlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    @InjectMocks
    private CandidatePlaceService candidatePlaceService;

    @Test
    @DisplayName("activityRadius가 있으면 날씨 반경보다 더 좁게 후보를 조회한다")
    void getInitialCandidates_usesPetActivityRadiusWhenItIsSmaller() {
        User user = baseUser();
        Pet pet = basePet(user, 3, null, Pet.PetActivity.NORMAL, null);
        WeatherContext weather = goodWeather();

        when(placeRepository.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt())).thenReturn(List.of());

        candidatePlaceService.getInitialCandidates(user, pet, weather, user.getLatitude(), user.getLongitude());

        verify(placeRepository).findNearby(
                eq(user.getLatitude()),
                eq(user.getLongitude()),
                eq(3000),
                eq(120)
        );
    }

    @Test
    @DisplayName("선호 장소와 overview 신호가 맞는 후보를 앞쪽에 둔다")
    void getInitialCandidates_prioritizesPreferredPlaceUsingOverviewSignals() {
        User user = baseUser();
        Pet pet = basePet(user, null, "실내카페", Pet.PetActivity.LOW, "조용하고 예민함");
        WeatherContext weather = goodWeather();

        Place outdoorPark = Place.builder()
                .id(1L)
                .title("Outdoor Park")
                .address("서울")
                .latitude(37.57)
                .longitude(126.98)
                .category("PLACE")
                .overview("넓은 공원과 산책로가 이어지는 야외 장소입니다.")
                .blogPositiveTags("넓음,산책,야외")
                .isVerified(true)
                .build();

        Place indoorCafe = Place.builder()
                .id(2L)
                .title("Indoor Cafe")
                .address("서울")
                .latitude(37.58)
                .longitude(126.99)
                .category("DINING")
                .overview("조용하게 쉬기 좋은 실내 카페로 휴식하기 좋습니다.")
                .blogPositiveTags("조용,실내,휴식")
                .isVerified(true)
                .build();

        when(placeRepository.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(outdoorPark, indoorCafe));
        List<Place> candidates = candidatePlaceService.getInitialCandidates(
                user,
                pet,
                weather,
                user.getLatitude(),
                user.getLongitude()
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).getId()).isEqualTo(2L);
    }

    private User baseUser() {
        return User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();
    }

    private Pet basePet(User user, Integer activityRadius, String preferredPlace, Pet.PetActivity activity, String personality) {
        return Pet.builder()
                .petId(10L)
                .user(user)
                .petName("mong")
                .petBreed("maltese")
                .petSize(Pet.PetSize.SMALL)
                .petAge(5)
                .petActivity(activity)
                .activityRadius(activityRadius)
                .preferredPlace(preferredPlace)
                .personality(personality)
                .isRepresentative(true)
                .build();
    }

    private WeatherContext goodWeather() {
        return WeatherContext.builder()
                .temperature(22.0)
                .humidity(45)
                .precipitationType("NONE")
                .windSpeed(1.5)
                .raining(false)
                .cold(false)
                .hot(false)
                .windy(false)
                .walkLevel("GOOD")
                .build();
    }
}
