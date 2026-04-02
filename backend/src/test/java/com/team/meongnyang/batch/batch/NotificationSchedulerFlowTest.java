package com.team.meongnyang.batch.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.batch.NotificationBatchService;
import com.team.meongnyang.recommendation.batch.NotificationScheduler;
import com.team.meongnyang.recommendation.batch.WeatherBatchService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerFlowTest {

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
    @Mock
    private WeatherBatchService weatherBatchService;

    @InjectMocks
    private NotificationBatchService notificationBatchService;

    @Test
    @DisplayName("스케줄러 실행부터 알림 발송 성공 처리까지 한 흐름으로 수행한다")
    void runDailyNotificationBatch_endToEndFlow() {
        NotificationScheduler notificationScheduler =
                new NotificationScheduler(notificationBatchService, weatherBatchService);

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
                .build();

        NotificationResponse notificationResponse = NotificationResponse.builder()
                .success(true)
                .requestId("request-id")
                .requestTime("2026-04-02T09:00:00")
                .statusCode("0000")
                .statusName("success")
                .build();

        when(userRepository.findAllByNotificationEnabledTrueAndStatusAndRole(User.Status.ACTIVE, User.Role.USER))
                .thenReturn(List.of(user));
        when(dailyRecommendationCacheService.isSentToday(null)).thenReturn(false);
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(petRepository.findAllByUserUserIdInAndIsRepresentativeTrueAndUserStatusAndUserRole(
                List.of(user.getUserId()),
                User.Status.ACTIVE,
                User.Role.USER
        )).thenReturn(List.of(pet));
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

        notificationScheduler.runDailyNotificationBatch();

        verify(recommendationPipelineService, times(1))
                .recommendForNotification(eq(user), eq(pet), anyString());
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
                .lastNotificationSentAt(null)
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
