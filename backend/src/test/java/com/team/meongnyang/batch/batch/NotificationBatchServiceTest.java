package com.team.meongnyang.batch.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.batch.NotificationBatchService;
import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.notification.service.NotificationService;
import com.team.meongnyang.recommendation.service.RecommendationPipelineService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationBatchServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private RecommendationPipelineService recommendationPipelineService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DailyRecommendationCacheService dailyRecommendationCacheService;

    @InjectMocks
    private NotificationBatchService notificationBatchService;

    @Test
    @DisplayName("알림 전송 성공 후 daily recommendation cache를 저장한다")
    void runDailyNotificationBatch_success() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        Place place = fixturePlace();

        RecommendationNotificationResult recommendationResult = RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("SUNNY")
                .weatherWalkLevel("GOOD")
                .weatherSummary("sunny day")
                .place(place)
                .message("alpha park is a good choice")
                .fallbackUsed(false)
                .cacheHit(false)
                .aiResponse("full gemini response")
                .geminiCacheKey("gemini:v2:model:hash")
                .build();

        NotificationResponse notificationResponse = NotificationResponse.builder()
                .success(true)
                .requestId("request-id")
                .requestTime("2026-03-24T09:00:00")
                .statusCode("202")
                .statusName("success")
                .build();

        when(userRepository.findAllByNotificationEnabledTrueAndStatusAndRole(User.Status.ACTIVE, User.Role.USER))
                .thenReturn(List.of(user));
        when(petRepository.findAllByUserUserIdInAndIsRepresentativeTrueAndUserStatusAndUserRole(
                List.of(user.getUserId()),
                User.Status.ACTIVE,
                User.Role.USER
        ))
                .thenReturn(List.of(pet));
        when(recommendationPipelineService.recommendForNotification(eq(user), eq(pet), anyString()))
                .thenReturn(recommendationResult);
        when(notificationService.send(
                user,
                pet,
                place,
                recommendationResult.getMessage(),
                recommendationResult.getWeatherType()
        )).thenReturn(notificationResponse);
        when(userRepository.save(any(User.class))).thenReturn(user);

        notificationBatchService.runDailyNotificationBatch();

        verify(recommendationPipelineService, times(1)).recommendForNotification(eq(user), eq(pet), anyString());
        verify(notificationService, times(1)).send(
                user,
                pet,
                place,
                recommendationResult.getMessage(),
                recommendationResult.getWeatherType()
        );
        verify(userRepository, times(1)).save(any(User.class));
        verify(dailyRecommendationCacheService, times(1))
                .saveToday(eq(user.getUserId()), eq(recommendationResult), anyString());
    }

    @Test
    @DisplayName("추천 결과가 없으면 알림 전송과 캐시 저장을 하지 않는다")
    void runDailyNotificationBatch_noCandidate() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);

        RecommendationNotificationResult recommendationResult = RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("SUNNY")
                .weatherWalkLevel("GOOD")
                .weatherSummary("sunny day")
                .place(null)
                .message("추천 가능한 장소가 없습니다.")
                .fallbackUsed(false)
                .cacheHit(false)
                .build();

        when(userRepository.findAllByNotificationEnabledTrueAndStatusAndRole(User.Status.ACTIVE, User.Role.USER))
                .thenReturn(List.of(user));
        when(petRepository.findAllByUserUserIdInAndIsRepresentativeTrueAndUserStatusAndUserRole(
                List.of(user.getUserId()),
                User.Status.ACTIVE,
                User.Role.USER
        ))
                .thenReturn(List.of(pet));
        when(recommendationPipelineService.recommendForNotification(eq(user), eq(pet), anyString()))
                .thenReturn(recommendationResult);

        notificationBatchService.runDailyNotificationBatch();

        verify(notificationService, never()).send(any(User.class), any(Pet.class), any(Place.class), anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(dailyRecommendationCacheService, never()).saveToday(any(Long.class), any(RecommendationNotificationResult.class), anyString());
    }

    private User fixtureUser() {
        return User.builder()
                .userId(1L)
                .email("batch-user@example.com")
                .password("encoded-password")
                .nickname("batch-user")
                .phoneNumber("01012345678")
                .notificationEnabled(true)
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
    }

    private Pet fixturePet(User user) {
        return Pet.builder()
                .petId(10L)
                .user(user)
                .petName("mong")
                .petBreed("breed")
                .petSize(Pet.PetSize.SMALL)
                .petAge(3)
                .petActivity(Pet.PetActivity.NORMAL)
                .isRepresentative(true)
                .build();
    }

    private Place fixturePlace() {
        return Place.builder()
                .id(100L)
                .contentId("content-100")
                .isVerified(true)
                .title("Alpha Park")
                .description("recommended place")
                .address("Suwon")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .rating(4.5)
                .reviewCount(10)
                .tags("outdoor,park")
                .build();
    }
}
