package com.team.meongnyang.orchestrator.controller;

import com.team.meongnyang.orchestrator.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrchController {

  private final OrchestratorService service;

  @GetMapping("/orch/test")
  public String test () {
    return service.recommend(2L);
  }
}
