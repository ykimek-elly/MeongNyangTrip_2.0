package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.recommendation.cache.DailyRecommendationCacheService;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecommendationQueryService {

  private final RecommendationUserReader recommendationUserReader;
  private final RecommendationPipelineService recommendationPipelineService;
  private final DailyRecommendationCacheService dailyRecommendationCacheService;

  public RecommendationNotificationResult getRecommendationForCurrentUser(String email) {
    User user = recommendationUserReader.getCurrentUserByEmail(email);

    if (!dailyRecommendationCacheService.isSentToday(user.getLastNotificationSentAt())) {
      return recommendationPipelineService.recommendForCurrentUser(email);
    }

    RecommendationNotificationResult cachedResult = dailyRecommendationCacheService.getTodayResult(user.getUserId());
    if (cachedResult != null) {
      log.info("[RecommendationQueryService] 오늘 배치 추천 결과 재사용 userId={}", user.getUserId());
      return cachedResult;
    }

    log.info("[RecommendationQueryService] 오늘 배치 발송 이력은 있으나 daily cache가 없어 재생성 userId={}", user.getUserId());
    return recommendationPipelineService.recommendForCurrentUser(email);
  }
}
