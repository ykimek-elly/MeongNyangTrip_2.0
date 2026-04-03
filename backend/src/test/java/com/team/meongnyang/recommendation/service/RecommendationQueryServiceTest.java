package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.dto.RecommendationLookupResponse;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecommendationQueryServiceTest {

    @Mock
    private RecommendationUserReader recommendationUserReader;

    @Mock
    private RecommendationPipelineService recommendationPipelineService;

    @Mock
    private DailyRecommendationCacheService dailyRecommendationCacheService;

    @Mock
    private RecommendationAiResponseParser aiResponseParser;

    @InjectMocks
    private RecommendationQueryService recommendationQueryService;

    @Test
    @DisplayName("오늘 추천 캐시가 있으면 배치 결과를 그대로 재사용한다")
    void returnsTodayCachedRecommendationFirst() {
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .build();
        RecommendationNotificationResult cachedResult = RecommendationNotificationResult.builder()
                .userId(1L)
                .petId(10L)
                .petName("몽이")
                .message("알림 요약")
                .recommendationDescription("추천 설명")
                .cacheHit(true)
                .build();

        given(recommendationUserReader.getCurrentUserByEmail("user@test.com")).willReturn(user);
        given(dailyRecommendationCacheService.isSentToday(1L, null)).willReturn(true);
        given(dailyRecommendationCacheService.getTodayResult(1L)).willReturn(cachedResult);

        RecommendationLookupResponse response =
                recommendationQueryService.getRecommendationForCurrentUser("user@test.com");

        assertThat(response.getNotificationSummary()).isEqualTo("알림 요약");
        assertThat(response.getRecommendationDescription()).isEqualTo("추천 설명");
        assertThat(response.isCacheHit()).isTrue();
        verifyNoInteractions(recommendationPipelineService);
    }

    @Test
    @DisplayName("기존 캐시 결과에 추천 설명이 비어 있으면 AI 원문에서 추천 설명을 복원한다")
    void restoresRecommendationDescriptionFromAiResponse() {
        User user = User.builder()
                .userId(3L)
                .email("user@test.com")
                .build();
        String aiResponse = """
                [추천설명]
                아차산 어울림정원은 산책하기 편한 흐름 덕분에 야외 동선을 활용하기 좋습니다.

                [알림요약]
                아차산 어울림정원, 산책하기 편한 흐름으로 좋습니다.
                """;
        RecommendationNotificationResult cachedResult = RecommendationNotificationResult.builder()
                .userId(3L)
                .petId(30L)
                .petName("노을")
                .message("아차산 어울림정원, 산책하기 편한 흐름으로 좋습니다.")
                .recommendationDescription(null)
                .aiResponse(aiResponse)
                .build();

        given(recommendationUserReader.getCurrentUserByEmail("user@test.com")).willReturn(user);
        given(dailyRecommendationCacheService.isSentToday(3L, null)).willReturn(true);
        given(dailyRecommendationCacheService.getTodayResult(3L)).willReturn(cachedResult);
        given(aiResponseParser.extractRecommendationDescription(aiResponse))
                .willReturn("아차산 어울림정원은 산책하기 편한 흐름 덕분에 야외 동선을 활용하기 좋습니다.");

        RecommendationLookupResponse response =
                recommendationQueryService.getRecommendationForCurrentUser("user@test.com");

        assertThat(response.getRecommendationDescription())
                .isEqualTo("아차산 어울림정원은 산책하기 편한 흐름 덕분에 야외 동선을 활용하기 좋습니다.");
    }

    @Test
    @DisplayName("오늘 추천 캐시가 없으면 실시간 추천으로 fallback 한다")
    void fallsBackToRealtimeRecommendationWhenCacheMissing() {
        User user = User.builder()
                .userId(2L)
                .email("user@test.com")
                .build();
        RecommendationNotificationResult pipelineResult = RecommendationNotificationResult.builder()
                .userId(2L)
                .petId(20L)
                .petName("냥이")
                .message("실시간 알림 요약")
                .recommendationDescription("실시간 추천 설명")
                .fallbackUsed(false)
                .cacheHit(false)
                .build();

        given(recommendationUserReader.getCurrentUserByEmail("user@test.com")).willReturn(user);
        given(dailyRecommendationCacheService.isSentToday(2L, null)).willReturn(false);
        given(recommendationPipelineService.recommendForCurrentUser("user@test.com")).willReturn(pipelineResult);

        RecommendationLookupResponse response =
                recommendationQueryService.getRecommendationForCurrentUser("user@test.com");

        assertThat(response.getNotificationSummary()).isEqualTo("실시간 알림 요약");
        assertThat(response.getRecommendationDescription()).isEqualTo("실시간 추천 설명");
        verify(recommendationPipelineService).recommendForCurrentUser("user@test.com");
    }
}
