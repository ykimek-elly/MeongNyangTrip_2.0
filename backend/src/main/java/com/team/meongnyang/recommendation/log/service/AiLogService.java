package com.team.meongnyang.recommendation.log.service;

import com.team.meongnyang.recommendation.log.entity.AiResponseLog;
import com.team.meongnyang.recommendation.log.repository.AiResponseLogRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * 추천 생성 과정에서 사용된 프롬프트와 AI 응답을 추적 가능한 형태로 저장하는 로그 서비스이다.
 *
 * <p>파이프라인 마지막 단계에서 캐시 적중 여부, fallback 사용 여부, 응답 지연 시간과 함께
 * AI 결과를 영속화한다. 저장된 로그는 추천 품질 분석, 장애 추적, 운영 모니터링에 활용된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiLogService {

  private final AiResponseLogRepository repository;

  /**
   * 추천 생성 결과와 실행 메타데이터를 AI 응답 로그로 저장한다.
   *
   * @param user 추천 대상 사용자 정보
   * @param pet 추천 기준이 된 반려동물 정보
   * @param prompt AI 호출에 사용한 최종 프롬프트
   * @param recommendedPlaces 로그용 상위 추천 장소 요약 문자열
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
          String ragContext,
          String responseText,
          boolean fallbackUsed,
          boolean cacheHit,
          Long latencyMs
  ) {
    String batchExecutionId = MDC.get("batchExecutionId");

    log.info("[AI 로그 저장] 저장 시작 batchExecutionId={}, userId={}, petId={}, fallbackUsed={}, cacheHit={}, latencyMs={}",
            batchExecutionId,
            user.getUserId(),
            pet.getPetId(),
            fallbackUsed,
            cacheHit,
            latencyMs);

    AiResponseLog aiResponseLog = AiResponseLog.builder()
            .userId(user.getUserId())
            .dogId(pet.getPetId())
            .batchExecutionId(batchExecutionId)
            .modelName("gemini-2.5-flash-lite")
            .prompt(prompt)
            .recommendedPlaces(recommendedPlaces)
            .ragContext(ragContext)
            .responseText(responseText)
            .fallbackUsed(fallbackUsed)
            .cacheHit(cacheHit)
            .latencyMs(latencyMs)
            .build();

    AiResponseLog savedLog = repository.save(aiResponseLog);
    log.info("[AI 로그 저장] 저장 완료 logId={}, batchExecutionId={}, userId={}, petId={}, fallbackUsed={}, cacheHit={}",
            savedLog.getId(),
            batchExecutionId,
            user.getUserId(),
            pet.getPetId(),
            fallbackUsed,
            cacheHit);
  }
}
