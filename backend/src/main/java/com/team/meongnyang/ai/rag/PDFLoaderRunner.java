package com.team.meongnyang.ai.rag;

import com.team.meongnyang.ai.rag.service.PDFLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PDFLoaderRunner implements CommandLineRunner {

  private final PDFLoaderService service;

  @Override
  public void run(String... args) throws Exception {
    log.info("[초기 PDF 적재 시작]");
    service.loadAllPdfs();
    log.info("[초기 PDF 적재 완료]");
  }
}
