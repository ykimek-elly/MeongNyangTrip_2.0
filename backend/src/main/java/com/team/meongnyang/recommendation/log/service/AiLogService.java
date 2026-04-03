package com.team.meongnyang.recommendation.log.service;

import com.team.meongnyang.recommendation.log.entity.AiResponseLog;
import com.team.meongnyang.recommendation.log.repository.AiResponseLogRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 추천 생성 과정에서 사용된 프롬프트와 AI 응답을 추적 가능한 형태로 저장하는 로그 서비스이다.
 *
 * 파이프라인 마지막 단계에서 캐시 적중 여부, fallback 사용 여부, 응답 지연 시간과 함께
 * AI 결과를 영속화한다. 저장된 로그는 추천 품질 분석, 장애 추적, 운영 모니터링에 활용된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiLogService {
  private static final double ONE_DAY_PENALTY = 25.0;
  private static final double SEVEN_DAY_PENALTY = 12.0;

  private final AiResponseLogRepository repository;

  /**
   * 추천 생성 결과와 실행 메타데이터를 AI 응답 로그로 저장한다.
   *
   * @param user 추천 대상 사용자 정보
   * @param pet 추천 기준이 된 반려동물 정보
   * @param prompt AI 호출에 사용한 최종 프롬프트
   * @param recommendedPlaces 로그용 상위 추천 장소 요약 문자열
   * @param recommendedPlaceId 실제 사용자에게 노출된 최상위 추천 장소 ID
   * @param ragContext 프롬프트에 반영한 RAG 문맥
   * @param responseText 사용자에게 반환한 AI 응답 문장
   * @param fallbackUsed fallback 문장 사용 여부
   * @param cacheHit 캐시 재사용 여부
   * @param latencyMs AI 응답 생성에 걸린 시간
   */
  public void save(
          User user,
          Pet pet,
          String prompt,
          String recommendedPlaces,
          Long recommendedPlaceId,
          String ragContext,
          String responseText,
          boolean fallbackUsed,
          boolean cacheHit,
          Long latencyMs
  ) {
    String batchExecutionId = MDC.get("batchExecutionId");

    AiResponseLog aiResponseLog = AiResponseLog.builder()
            .userId(user.getUserId())
            .dogId(pet.getPetId())
            .batchExecutionId(batchExecutionId)
            .modelName("gemini-2.5-flash-lite")
            .prompt(prompt)
            .recommendedPlaces(recommendedPlaces)
            .recommendedPlaceId(recommendedPlaceId)
            .ragContext(ragContext)
            .responseText(responseText)
            .fallbackUsed(fallbackUsed)
            .cacheHit(cacheHit)
            .latencyMs(latencyMs)
            .build();

    repository.save(aiResponseLog);
  }

  public Map<Long, RecommendationDiversityPenalty> getRecentRecommendedPlacePenalties(Long userId) {
    if (userId == null) {
      return Map.of();
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oneDayAgo = now.minusDays(1);
    LocalDateTime sevenDaysAgo = now.minusDays(7);

    List<AiResponseLog> recentLogs = repository.findByUserIdAndRegDateAfterOrderByRegDateDesc(userId, sevenDaysAgo);
    if (recentLogs.isEmpty()) {
      return Map.of();
    }

    // 최근 7일 로그를 순회하면서 장소별로 가장 강한 패널티만 유지한다.
    Map<Long, RecommendationDiversityPenalty> penalties = new LinkedHashMap<>();
    for (AiResponseLog logEntry : recentLogs) {
      Long placeId = logEntry.getRecommendedPlaceId();
      LocalDateTime recommendedAt = logEntry.getRegDate();
      if (placeId == null || recommendedAt == null) {
        continue;
      }

      RecommendationDiversityPenalty penalty = recommendedAt.isAfter(oneDayAgo)
              ? new RecommendationDiversityPenalty(ONE_DAY_PENALTY, "최근 1일 내 추천")
              : new RecommendationDiversityPenalty(SEVEN_DAY_PENALTY, "최근 7일 내 추천");

      penalties.merge(
              placeId,
              penalty,
              (current, incoming) -> current.penalty() >= incoming.penalty() ? current : incoming
      );
    }

    return penalties;
  }

  public record RecommendationDiversityPenalty(double penalty, String reason) {
  }
}
