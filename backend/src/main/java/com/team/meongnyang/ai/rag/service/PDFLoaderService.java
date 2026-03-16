package com.team.meongnyang.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PDFLoaderService {

  private final VectorStore vectorStore;
  private final JdbcTemplate jdbcTemplate;

  public void loadAllPdfs() {
    try {
      PathMatchingResourcePatternResolver resolver =
              new PathMatchingResourcePatternResolver();

      Resource[] resources = resolver.getResources("classpath:rag/*.pdf");

      log.info("발견된 PDF 수 : {}", resources.length);

      for (Resource resource : resources) {
        String filename = resource.getFilename();
        log.info("PDF 적재 시작 : {}", filename);

        loadPdf(resource);

        log.info("PDF 적재 완료 : {}", filename);
      }

    } catch (Exception e) {
      log.error("PDF 일괄 적재 실패", e);
    }
  }

  public void loadPdf(Resource resource) {
    String fileName = resource.getFilename();

    log.info("=====================================");
    log.info("[PDF 로드 시작] {}", fileName);
    log.info("=====================================");

    if (fileName == null || fileName.isBlank()) {
      throw new RuntimeException("fileName is null or blank");
    }
    if (alreadyLoaded(fileName)) {
      log.info("이미 적재된 파일입니다 : {} ", fileName);
      return;
    }

    TikaDocumentReader reader = new TikaDocumentReader(new ClassPathResource("rag/" + fileName));

    // PDF에서 Text 추출 후 Document 객체로 변환
    List<Document> documents = reader.get();
    log.info("원본 Document 개수 : {}", documents.size());

    // 긴 문서를 검색하기 좋은 작은 조각(chunk) 으로 나누는 설정
    TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500) // 한 청크 목표 크기 , 500 토큰 크기로 자름
            .withMinChunkSizeChars(150) // 너무 짧은 조각은 만들지 않게 최소 150자
            .withMinChunkLengthToEmbed(20) // 너무 짧은 텍스트는 임베딩 안 함
            .withMaxNumChunks(1000) // 최대 청크 수 제한
            .withKeepSeparator(true)  // 구분자 및 줄바꿈 등 구분 정보 유지
            .build();

    // 분할된 Document 객체
    List<Document> splitDocuments = splitter.apply(documents);
    log.info("분할 Document 개수 : {}", splitDocuments.size());

    for (Document doc : splitDocuments) {
      doc.getMetadata().put("source", fileName);
      doc.getMetadata().put("category", inferCategory(fileName));
    }

    // DB에 저장
    vectorStore.add(splitDocuments);

    log.info("[벡터 저장 완료] fileName : {} , count : {}",fileName, splitDocuments.size());
  }

  private boolean alreadyLoaded (String fileName) {
    Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM public.vector_store WHERE metadata->>'source' = ?",
            Integer.class,
            fileName
    );
    return count != null && count > 0;
  }

  private String inferCategory(String fileName) {
    if (fileName.contains("산책")) return "walk";
    if (fileName.contains("건강")) return "health";
    if (fileName.contains("영양")) return "nutrition";
    if (fileName.contains("공공장소") || fileName.contains("동물복지")) return "etiquette";
    return "common";
  }
}
