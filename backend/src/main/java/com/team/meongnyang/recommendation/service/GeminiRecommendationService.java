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
  private static final String FALLBACK_MESSAGE = "?꾩옱 議곌굔??諛뷀깢?쇰줈 異붿쿇??以鍮꾪뻽?댁슂. ?ㅻ뒛? ?좎뵪? 諛섎젮寃??곹깭瑜?怨좊젮??臾대━ ?녿뒗 媛源뚯슫 ?μ냼遺??媛蹂띻쾶 ?섎윭蹂대뒗 寃껋쓣 異붿쿇?쒕젮??";

  private final ChatClient chatClient;
  public boolean isFallbackResponse(String response) {
    return createFallbackMessage().equals(response);
  }

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

  /**
   * Gemini 호출 실패 시 사용할 기본 안내 문장을 생성한다.
   *
   * @return fallback 문장
   */
  private String createFallbackMessage() {
    return "현재 조건을 바탕으로 추천을 준비했어요. 오늘은 날씨와 반려견 상태를 고려해 무리 없는 가까운 장소부터 가볍게 둘러보는 것을 추천드려요.";
  }
}
