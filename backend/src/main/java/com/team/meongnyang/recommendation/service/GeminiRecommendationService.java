package com.team.meongnyang.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Gemini를 호출해 추천 문장을 생성하는 서비스
 * AI 응답 생성, 타임아웃 제어, 재시도 처리, fallback 응답 반환을 담당한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeminiRecommendationService {
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

  /**
   * 주어진 응답이 fallback 문장인지 확인한다.
   */
  public boolean isFallbackResponse(String response) {
    return createFallbackMessage().equals(response);
  }

  /**
   * 프롬프트를 기반으로 추천 문장을 생성한다.
   */
  public String generateRecommendation(String prompt) {
    for (int attempt = 1; attempt <= Math.max(geminiMaxAttempts, 1); attempt++) {
      try {
        Future<String> future = geminiCallExecutor.submit(() -> chatClient.prompt()
                .user(prompt)
                .call()
                .content());

        String content = future.get(geminiTimeoutMs, TimeUnit.MILLISECONDS);
        if (content == null || content.isBlank()) {
          log.warn("[AI 호출] 빈 응답 fallback userId={}, petId={}, batchExecutionId={}, attempt={}",
                  RecommendationLogContext.userId(),
                  RecommendationLogContext.petId(),
                  RecommendationLogContext.batchExecutionId(),
                  attempt);
          return createFallbackMessage();
        }
        return content;
      } catch (TimeoutException e) {
        log.warn("[AI 호출] timeout 재시도 userId={}, petId={}, batchExecutionId={}, attempt={}, timeoutMs={}",
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                attempt,
                geminiTimeoutMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[AI 호출] 인터럽트 fallback userId={}, petId={}, batchExecutionId={}, attempt={}",
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                attempt);
        return createFallbackMessage();
      } catch (ExecutionException e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        log.warn("[AI 호출] 예외 재시도 userId={}, petId={}, batchExecutionId={}, attempt={}, errorType={}",
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                attempt,
                cause.getClass().getSimpleName());
      } catch (Exception e) {
        log.warn("[AI 호출] 예외 재시도 userId={}, petId={}, batchExecutionId={}, attempt={}, errorType={}",
                RecommendationLogContext.userId(),
                RecommendationLogContext.petId(),
                RecommendationLogContext.batchExecutionId(),
                attempt,
                e.getClass().getSimpleName());
      }

      if (attempt < Math.max(geminiMaxAttempts, 1)) {
        sleepBeforeRetry();
      }
    }

    log.warn("[AI 호출] 최대 재시도 초과 fallback userId={}, petId={}, batchExecutionId={}, maxAttempts={}",
            RecommendationLogContext.userId(),
            RecommendationLogContext.petId(),
            RecommendationLogContext.batchExecutionId(),
            geminiMaxAttempts);
    return createFallbackMessage();
  }

  /**
   * AI 호출 실패 시 사용할 기본 안내 문장을 반환한다.
   */
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

  /**
   * 서비스 종료 시 AI 호출용 실행기를 정리한다.
   */
  @PreDestroy
  void shutdownExecutor() {
    geminiCallExecutor.close();
  }
}
