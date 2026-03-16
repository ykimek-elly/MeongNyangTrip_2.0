package com.team.meongnyang.recommendation.log.service;

import com.team.meongnyang.recommendation.log.entity.AiResponseLog;
import com.team.meongnyang.recommendation.log.repository.AiResponseLogRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiLogService {

  private final AiResponseLogRepository repository;

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
    log.info("[AI 로그 저장] 저장 시작 userId={}, petId={}, fallbackUsed={}, cacheHit={}, latencyMs={}",
            user.getUserId(),
            pet.getPetId(),
            fallbackUsed,
            cacheHit,
            latencyMs);

    AiResponseLog aiResponseLog = AiResponseLog.builder()
            .userId(user.getUserId())
            .dogId(pet.getPetId())
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
    log.info("[AI 로그 저장] 저장 완료 logId={}, userId={}, petId={}, fallbackUsed={}, cacheHit={}",
            savedLog.getId(),
            user.getUserId(),
            pet.getPetId(),
            fallbackUsed,
            cacheHit);
  }
}
