package com.team.meongnyang.recommendation.controller;

import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.service.RecommendationPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

  private final RecommendationPipelineService service;

  @GetMapping("/api/v1/ai/walk-guide")
  public RecommendationNotificationResult recommendForCurrentUser() {
    // TODO : 인증 기능 (시큐리티 인증 객체에서 EMAIL or USERNAME)
    String email = "testsingle2@test.com";

    return service.recommendForCurrentUser(email);
  }
}
