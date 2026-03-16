package com.team.meongnyang.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeminiService {

  private final ChatClient chatClient;
  /**
   * 프롬프트를 기반으로 Gemini를 호출해 최종 추천 문장을 생성한다.
   *
   * 처리:
   * - 정상 응답 시 AI 생성 문장 반환
   * - 예외 발생 시 fallback 문장 반환
   *
   * @param prompt Gemini 입력 프롬프트
   * @return 최종 사용자 안내 문장
   */
  public String generateRecommendation(String prompt) {
    try {
      String content = chatClient.prompt()
                                 .user(prompt)
                                 .call()
                                 .content();

      if (content == null || content.isBlank()) {
        log.warn("Gemini 응답이 비어 있어 fallback 문장을 반환합니다.");
        return createFallbackMessage();
      }

      return content;

    } catch (Exception e) {
      log.error("Gemini 호출 중 예외 발생", e);
      return createFallbackMessage();
    }
  }

  /**
   * Gemini 호출 실패 시 사용할 기본 안내 문장을 생성한다.
   *
   * @return fallback 문장
   */
  private String createFallbackMessage() {
    return "현재 조건을 바탕으로 추천을 준비했어요. 오늘은 날씨와 반려견 상태를 고려해 무리 없는 가까운 장소부터 가볍게 둘러보는 것을 추천드려요.";
  }
}
