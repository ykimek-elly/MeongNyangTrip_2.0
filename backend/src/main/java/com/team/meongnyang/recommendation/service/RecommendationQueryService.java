package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 추천 조회 진입점을 담당한다.
 *
 * <p>오늘 이미 발송된 추천이 있으면 daily cache를 우선 사용하고,
 * 없으면 실시간 추천 파이프라인으로 위임한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecommendationQueryService {

    private final RecommendationUserReader recommendationUserReader;
    private final RecommendationPipelineService recommendationPipelineService;
    private final DailyRecommendationCacheService dailyRecommendationCacheService;

    /**
     * 현재 사용자에게 보여줄 추천 결과를 반환한다.
     *
     * @param email 사용자 이메일
     * @return 추천 결과
     */
    public RecommendationNotificationResult getRecommendationForCurrentUser(String email) {
        User user = recommendationUserReader.getCurrentUserByEmail(email);

        if (!dailyRecommendationCacheService.isSentToday(user.getUserId(), user.getLastNotificationSentAt())) {
            return recommendationPipelineService.recommendForCurrentUser(email);
        }

        RecommendationNotificationResult cachedResult = dailyRecommendationCacheService.getTodayResult(user.getUserId());
        if (cachedResult != null) {
            log.info("[RecommendationQueryService] 오늘 배치 추천 결과를 재사용합니다. userId={}", user.getUserId());
            return cachedResult;
        }

        log.info("[RecommendationQueryService] 오늘 발송 이력은 있지만 daily cache가 없어 실시간 추천을 수행합니다. userId={}",
                user.getUserId());
        return recommendationPipelineService.recommendForCurrentUser(email);
    }
}
