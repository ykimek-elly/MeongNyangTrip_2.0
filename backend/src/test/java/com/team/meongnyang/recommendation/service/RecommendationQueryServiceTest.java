package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationQueryServiceTest {

    @Mock
    private RecommendationUserReader recommendationUserReader;
    @Mock
    private RecommendationPipelineService recommendationPipelineService;
    @Mock
    private DailyRecommendationCacheService dailyRecommendationCacheService;

    @InjectMocks
    private RecommendationQueryService recommendationQueryService;

    @Test
    @DisplayName("오늘 배치 발송 이력이 있으면 daily cache 결과를 그대로 반환한다")
    void getRecommendationForCurrentUser_usesDailyCache() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .nickname("tester")
                .lastNotificationSentAt(LocalDateTime.now())
                .build();

        RecommendationNotificationResult cachedResult = RecommendationNotificationResult.builder()
                .userId(1L)
                .petId(10L)
                .petName("mong")
                .message("cached message")
                .build();

        when(recommendationUserReader.getCurrentUserByEmail("user@example.com")).thenReturn(user);
        when(dailyRecommendationCacheService.isSentToday(user.getLastNotificationSentAt())).thenReturn(true);
        when(dailyRecommendationCacheService.getTodayResult(user.getUserId())).thenReturn(cachedResult);

        RecommendationNotificationResult result =
                recommendationQueryService.getRecommendationForCurrentUser("user@example.com");

        assertThat(result).isSameAs(cachedResult);
        verify(recommendationPipelineService, never()).recommendForCurrentUser("user@example.com");
    }

    @Test
    @DisplayName("오늘 배치 발송 이력이 없으면 기존 추천 파이프라인을 실행한다")
    void getRecommendationForCurrentUser_runsPipelineWhenNoDailyCacheEligibility() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .nickname("tester")
                .build();

        RecommendationNotificationResult pipelineResult = RecommendationNotificationResult.builder()
                .userId(1L)
                .message("new recommendation")
                .build();

        when(recommendationUserReader.getCurrentUserByEmail("user@example.com")).thenReturn(user);
        when(dailyRecommendationCacheService.isSentToday(null)).thenReturn(false);
        when(recommendationPipelineService.recommendForCurrentUser("user@example.com")).thenReturn(pipelineResult);

        RecommendationNotificationResult result =
                recommendationQueryService.getRecommendationForCurrentUser("user@example.com");

        assertThat(result).isSameAs(pipelineResult);
    }
}
