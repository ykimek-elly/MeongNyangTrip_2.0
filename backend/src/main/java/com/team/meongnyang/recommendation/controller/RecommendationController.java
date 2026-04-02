package com.team.meongnyang.recommendation.controller;

import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.service.RecommendationAuthenticationService;
import com.team.meongnyang.recommendation.service.RecommendationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

  private final RecommendationQueryService recommendationQueryService;
  private final RecommendationAuthenticationService authenticationService;

  @GetMapping("/api/v1/ai/walk-guide")
  public RecommendationNotificationResult recommendForUser(Authentication authentication) {
    String email = authenticationService.getAuthenticatedUserEmail(authentication);
    return recommendationQueryService.getRecommendationForCurrentUser(email);
  }
}
