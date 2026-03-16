package com.team.meongnyang.ai.service;

import com.team.meongnyang.ai.entity.AiResponseLog;
import com.team.meongnyang.ai.repository.AiResponseLogRepository;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiLogservice {

  private final AiResponseLogRepository repository;


  public void save(
          User user,
          Dog dog,
          String prompt,
          String recommendedPlaces,
          String ragContext,
          String responseText,
          boolean fallbackUsed,
          boolean cacheHit,
          Long latencyMs
  ) {

    AiResponseLog log = AiResponseLog.builder()
            .userId(user.getUserId())
            .dogId(dog.getDogId())
            .modelName("gemini-2.5-flash-lite")
            .prompt(prompt)
            .recommendedPlaces(recommendedPlaces)
            .ragContext(ragContext)
            .responseText(responseText)
            .fallbackUsed(fallbackUsed)
            .cacheHit(cacheHit)
            .latencyMs(latencyMs)
            .build();

    repository.save(log);
  }
}
