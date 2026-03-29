package com.team.meongnyang.recommendation.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.dto.RecommendationResultCachePayload;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationResultCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RecommendationCachePolicy recommendationCachePolicy;
    private RecommendationResultCacheService recommendationResultCacheService;

    @BeforeEach
    void setUp() {
        recommendationCachePolicy = new RecommendationCachePolicy();
        ReflectionTestUtils.setField(recommendationCachePolicy, "recommendationCacheTtl", Duration.ofMinutes(10));
        recommendationResultCacheService = new RecommendationResultCacheService(
                redisTemplate,
                new ObjectMapper(),
                recommendationCachePolicy
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("추천 결과 캐시는 Place 엔티티 대신 캐시 전용 DTO를 저장한다")
    void save_convertsEntityToCachePayload() {
        RecommendationNotificationResult result = RecommendationNotificationResult.builder()
                .userId(1L)
                .petId(2L)
                .petName("mong")
                .weatherType("SUNNY")
                .weatherWalkLevel("GOOD")
                .weatherSummary("clear")
                .place(fixturePlace())
                .message("go outside")
                .fallbackUsed(false)
                .cacheHit(false)
                .error(false)
                .aiResponse("ai raw")
                .geminiCacheKey("gemini:key")
                .build();

        recommendationResultCacheService.save("recommendation:key", result);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("recommendation:key"),
                payloadCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(10))
        );

        assertThat(payloadCaptor.getValue()).isInstanceOf(RecommendationResultCachePayload.class);
        RecommendationResultCachePayload savedPayload =
                (RecommendationResultCachePayload) payloadCaptor.getValue();
        assertThat(savedPayload.getPlace()).isNotNull();
        assertThat(savedPayload.getPlace().getId()).isEqualTo(100L);
        assertThat(savedPayload.getGeminiCacheKey()).isEqualTo("gemini:key");
    }

    @Test
    @DisplayName("추천 결과 캐시는 캐시 DTO를 읽어 RecommendationNotificationResult로 복원한다")
    void get_restoresNotificationResultFromCachePayload() {
        RecommendationResultCachePayload payload = RecommendationResultCachePayload.builder()
                .userId(1L)
                .petId(2L)
                .petName("mong")
                .weatherType("SUNNY")
                .weatherWalkLevel("GOOD")
                .weatherSummary("clear")
                .place(RecommendationResultCachePayload.from(
                        RecommendationNotificationResult.builder().place(fixturePlace()).build()
                ).getPlace())
                .message("go outside")
                .fallbackUsed(false)
                .cacheHit(true)
                .error(false)
                .aiResponse("ai raw")
                .geminiCacheKey("gemini:key")
                .build();
        when(valueOperations.get("recommendation:key")).thenReturn(payload);

        RecommendationNotificationResult result = recommendationResultCacheService.get("recommendation:key");

        assertThat(result).isNotNull();
        assertThat(result.getPlace()).isNotNull();
        assertThat(result.getPlace().getId()).isEqualTo(100L);
        assertThat(result.getPlace().getLatitude()).isEqualTo(37.27);
        assertThat(result.getPlace().getLongitude()).isEqualTo(127.01);
        assertThat(result.getGeminiCacheKey()).isEqualTo("gemini:key");
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
