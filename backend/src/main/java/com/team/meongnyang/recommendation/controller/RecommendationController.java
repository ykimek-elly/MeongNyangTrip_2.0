package com.team.meongnyang.recommendation.controller;

import com.team.meongnyang.recommendation.dto.RecommendationLookupResponse;
import com.team.meongnyang.recommendation.service.RecommendationAuthenticationService;
import com.team.meongnyang.recommendation.service.RecommendationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 요청 기반으로 산책 추천 결과를 제공하는 컨트롤러
 * 인증된 사용자 정보를 기반으로 추천 서비스를 호출한다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

  private final RecommendationQueryService recommendationQueryService;
  private final RecommendationAuthenticationService authenticationService;

  /**
   * 현재 로그인한 사용자에게 추천 결과를 반환한다.
   */
  @GetMapping("/api/v1/ai/walk-guide")
  public RecommendationLookupResponse recommendForUser(Authentication authentication) {
    String email = authenticationService.getAuthenticatedUserEmail(authentication);
    return recommendationQueryService.getRecommendationForCurrentUser(email);
  }
}
