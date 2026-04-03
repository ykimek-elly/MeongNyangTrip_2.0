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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatePlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    @Spy
    private ActivityRadiusPolicy activityRadiusPolicy = new ActivityRadiusPolicy();

    @InjectMocks
    private CandidatePlaceService candidatePlaceService;

    @Test
    @DisplayName("user activity radius drives the first nearby query")
    void getInitialCandidates_usesUserActivityRadius() {
        User user = baseUser(5);
        Pet pet = basePet(user, 3, null, Pet.PetActivity.NORMAL, null);
        WeatherContext weather = goodWeather();

        when(placeRepository.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt())).thenReturn(List.of());

        candidatePlaceService.getInitialCandidates(user, pet, weather, user.getLatitude(), user.getLongitude());

        verify(placeRepository).findNearby(
                eq(user.getLatitude()),
                eq(user.getLongitude()),
                eq(5000),
                eq(120)
        );
    }

    @Test
    @DisplayName("missing user activity radius falls back to 15km")
    void getInitialCandidates_usesDefaultRadiusWhenUserActivityRadiusMissing() {
        User user = baseUser(null);
        Pet pet = basePet(user, null, null, Pet.PetActivity.NORMAL, null);
        WeatherContext weather = goodWeather();

        when(placeRepository.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt())).thenReturn(List.of());

        candidatePlaceService.getInitialCandidates(user, pet, weather, user.getLatitude(), user.getLongitude());

        verify(placeRepository).findNearby(
                eq(user.getLatitude()),
                eq(user.getLongitude()),
                eq(15000),
                eq(120)
        );
    }

    @Test
    @DisplayName("out-of-range user activity radius is clamped")
    void getInitialCandidates_clampsUserActivityRadius() {
        User tooSmallUser = baseUser(1);
        User tooLargeUser = baseUser(100);
        Pet pet = basePet(tooSmallUser, null, null, Pet.PetActivity.NORMAL, null);
        WeatherContext weather = goodWeather();

        when(placeRepository.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(place(1L, "candidate", "PLACE", "park")))
                .thenReturn(List.of(place(2L, "candidate", "PLACE", "park")));

        candidatePlaceService.getInitialCandidates(tooSmallUser, pet, weather, tooSmallUser.getLatitude(), tooSmallUser.getLongitude());
        verify(placeRepository).findNearby(eq(tooSmallUser.getLatitude()), eq(tooSmallUser.getLongitude()), eq(3000), eq(120));

        clearInvocations(placeRepository);

        candidatePlaceService.getInitialCandidates(tooLargeUser, pet, weather, tooLargeUser.getLatitude(), tooLargeUser.getLongitude());
        verify(placeRepository).findNearby(eq(tooLargeUser.getLatitude()), eq(tooLargeUser.getLongitude()), eq(50000), eq(120));
    }

    @Test
    @DisplayName("zero nearby candidates expand the query radius step by step")
    void getInitialCandidates_expandsRadiusWhenNoCandidatesFound() {
        User user = baseUser(5);
        Pet pet = basePet(user, null, null, Pet.PetActivity.NORMAL, null);
        WeatherContext weather = goodWeather();

        when(placeRepository.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of(place(1L, "fallback place", "PLACE", "park walk")));

        List<Place> candidates = candidatePlaceService.getInitialCandidates(
                user,
                pet,
                weather,
                user.getLatitude(),
                user.getLongitude()
        );

        assertThat(candidates).hasSize(1);
        verify(placeRepository).findNearby(eq(user.getLatitude()), eq(user.getLongitude()), eq(5000), eq(120));
        verify(placeRepository).findNearby(eq(user.getLatitude()), eq(user.getLongitude()), eq(15000), eq(120));
        verify(placeRepository).findNearby(eq(user.getLatitude()), eq(user.getLongitude()), eq(30000), eq(120));
    }

    @Test
    @DisplayName("preferred place signals are prioritized")
    void getInitialCandidates_prioritizesPreferredPlaceUsingOverviewSignals() {
        User user = baseUser(15);
        Pet pet = basePet(user, null, "indoor cafe", Pet.PetActivity.LOW, "quiet calm");
        WeatherContext weather = goodWeather();

        Place outdoorPark = place(1L, "outdoor park", "PLACE", "outdoor park trail");
        Place indoorCafe = place(2L, "indoor cafe", "DINING", "quiet indoor cafe rest");

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

    private User baseUser(Integer activityRadius) {
        return User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .latitude(37.5665)
                .longitude(126.9780)
                .activityRadius(activityRadius)
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

    private Place place(Long id, String title, String category, String tags) {
        return Place.builder()
                .id(id)
                .title(title)
                .address("seoul")
                .latitude(37.57)
                .longitude(126.98)
                .category(category)
                .overview(tags)
                .tags(tags)
                .blogPositiveTags(tags)
                .isVerified(true)
                .build();
    }
}
