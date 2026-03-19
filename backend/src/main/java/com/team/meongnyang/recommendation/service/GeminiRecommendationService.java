package com.team.meongnyang.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeminiRecommendationService {
  private static final String FALLBACK_MESSAGE =
          "현재 조건을 바탕으로 추천을 준비했어요. 오늘은 날씨와 반려견 상태를 고려해 무리 없는 가까운 장소부터 가볍게 둘러보는 것을 추천드려요.";

  private final ChatClient chatClient;

  public boolean isFallbackResponse(String response) {
    return createFallbackMessage().equals(response);
  }

  public String generateRecommendation(String prompt) {
    log.info("[Gemini] 추천 문장 생성 시작 promptLength={}", prompt == null ? 0 : prompt.length());
    try {
      String content = chatClient.prompt()
                                 .user(prompt)
                                 .call()
                                 .content();

      if (content == null || content.isBlank()) {
        log.warn("[Gemini] 빈 응답으로 fallback 사용");
        return createFallbackMessage();
      }

      log.info("[Gemini] 추천 문장 생성 완료 responseLength={}", content.length());
      return content;

    } catch (Exception e) {
      log.error("Gemini 호출 중 예외 발생", e);
      log.warn("[Gemini] 예외 발생으로 fallback 사용 errorType={}", e.getClass().getSimpleName());
      return createFallbackMessage();
    }
  }

  private String createFallbackMessage() {
    return FALLBACK_MESSAGE;
  }
}
