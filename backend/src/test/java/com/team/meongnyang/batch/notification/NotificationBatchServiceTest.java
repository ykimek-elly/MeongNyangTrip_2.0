package com.team.meongnyang.batch.notification;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.dto.NotificationResponse;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.notification.service.KakaoNotificationService;
import com.team.meongnyang.recommendation.service.RecommendationPipelineService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
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
    private KakaoNotificationService kakaoNotificationService;

    @InjectMocks
    private NotificationBatchService notificationBatchService;

    @Test
    @DisplayName("배치 실행 시 추천 생성 후 알림 전송까지 성공한다")
    void runDailyNotificationBatch_success() {
        User user = fixtureUser();
        Pet pet = fixturePet(user);
        Place place = fixturePlace();

        RecommendationNotificationResult recommendationResult = RecommendationNotificationResult.builder()
                .userId(user.getUserId())
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .weatherType("SUNNY")
                .weatherSummary("산책하기 좋은 날씨")
                .place(place)
                .message("오늘은 공원 산책을 추천해요.")
                .fallbackUsed(false)
                .cacheHit(false)
                .build();

        NotificationResponse notificationResponse = NotificationResponse.builder()
                .success(true)
                .code("200")
                .message("mock success")
                .build();

        when(userRepository.findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE))
                .thenReturn(List.of(user));
        when(petRepository.findFirstByUser(user)).thenReturn(Optional.of(pet));
        when(recommendationPipelineService.recommendForNotification(user, pet)).thenReturn(recommendationResult);
        when(kakaoNotificationService.send(user, place, recommendationResult.getMessage())).thenReturn(notificationResponse);

        notificationBatchService.runDailyNotificationBatch();

        InOrder inOrder = inOrder(
                userRepository,
                petRepository,
                recommendationPipelineService,
                kakaoNotificationService
        );

        inOrder.verify(userRepository).findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE);
        inOrder.verify(petRepository).findFirstByUser(user);
        inOrder.verify(recommendationPipelineService).recommendForNotification(user, pet);
        inOrder.verify(kakaoNotificationService).send(user, place, recommendationResult.getMessage());

        verify(userRepository, times(1)).findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE);
        verify(petRepository, times(1)).findFirstByUser(user);
        verify(recommendationPipelineService, times(1)).recommendForNotification(user, pet);
        verify(kakaoNotificationService, times(1)).send(user, place, recommendationResult.getMessage());
    }

    private User fixtureUser() {
        return User.builder()
                .userId(1L)
                .email("batch-user@example.com")
                .password("encoded-password")
                .nickname("batch-user")
                .phoneNumber("01012345678")
                .notificationEnabled(true)
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
