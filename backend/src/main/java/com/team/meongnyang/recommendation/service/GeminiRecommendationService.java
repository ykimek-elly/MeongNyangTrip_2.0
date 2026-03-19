package com.team.meongnyang.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeminiRecommendationService {
  public static final String MODEL_NAME = "gemini-2.5-flash-lite";
  private static final String FALLBACK_MESSAGE =
          "현재 조건을 바탕으로 추천을 준비했어요. 오늘은 날씨와 반려견 상태를 고려해 무리 없는 가까운 장소부터 가볍게 둘러보는 것을 추천드려요.";

  private final ChatClient chatClient;
  private final ExecutorService geminiCallExecutor = Executors.newVirtualThreadPerTaskExecutor();

  @Value("${recommendation.gemini.timeout-ms:4000}")
  private long geminiTimeoutMs;

  @Value("${recommendation.gemini.retry.max-attempts:2}")
  private int geminiMaxAttempts;

  @Value("${recommendation.gemini.retry.backoff-ms:250}")
  private long geminiRetryBackoffMs;

  public boolean isFallbackResponse(String response) {
    return createFallbackMessage().equals(response);
  }

  public String generateRecommendation(String prompt) {
    log.info("[Gemini] 추천 문장 생성 시작 promptLength={}", prompt == null ? 0 : prompt.length());
    for (int attempt = 1; attempt <= Math.max(geminiMaxAttempts, 1); attempt++) {
      try {
        Future<String> future = geminiCallExecutor.submit(() -> chatClient.prompt()
                .user(prompt)
                .call()
                .content());

        String content = future.get(geminiTimeoutMs, TimeUnit.MILLISECONDS);
        if (content == null || content.isBlank()) {
          log.warn("[Gemini] 빈 응답으로 fallback 사용 attempt={}", attempt);
          return createFallbackMessage();
        }

        log.info("[Gemini] 추천 문장 생성 완료 responseLength={}, attempt={}", content.length(), attempt);
        return content;
      } catch (TimeoutException e) {
        log.warn("[Gemini] timeout 발생 attempt={}, timeoutMs={}", attempt, geminiTimeoutMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[Gemini] interrupted 발생 attempt={}", attempt);
        return createFallbackMessage();
      } catch (ExecutionException e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        log.warn("[Gemini] 실행 예외 발생 attempt={}, errorType={}", attempt, cause.getClass().getSimpleName());
      } catch (Exception e) {
        log.warn("[Gemini] 예외 발생 attempt={}, errorType={}", attempt, e.getClass().getSimpleName());
      }

      if (attempt < Math.max(geminiMaxAttempts, 1)) {
        sleepBeforeRetry();
      }
    }

    log.warn("[Gemini] 최대 재시도 초과로 fallback 사용 maxAttempts={}", geminiMaxAttempts);
    return createFallbackMessage();
  }

  private String createFallbackMessage() {
    return FALLBACK_MESSAGE;
  }

  /**
   * 외부 AI 호출 재시도 간격을 짧게 둬서 한 사용자 지연이 전체 배치 지연으로 커지지 않게 한다.
   */
  private void sleepBeforeRetry() {
    try {
      Thread.sleep(Math.max(geminiRetryBackoffMs, 0L));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @PreDestroy
  void shutdownExecutor() {
    geminiCallExecutor.close();
  }
}
