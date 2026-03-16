package com.team.meongnyang.recommendation.rag.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PDFLoaderRunner implements CommandLineRunner {

  private final PDFDocumentLoader service;

  @Override
  public void run(String... args) throws Exception {
    log.info("[초기 PDF 적재 시작]");
    service.loadAllPdfs();
    log.info("[초기 PDF 적재 완료]");
  }
}
