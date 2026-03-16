package com.team.meongnyang.orchestrator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 문서 검색 및 응답 생성 서비스
 *
 * 역할:
 * - 벡터 DB에서 질문과 유사한 문서를 검색
 * - 검색된 문서를 context 로 합쳐 Gemini에 전달
 * - 문서 기반 답변 생성
 *
 * 현재 사용 목적:
 * - 반려견 상태 + 날씨 조건에 맞는 참고 문서 검색
 * - 추천 문장 생성 시 참고할 근거 정보 확보
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RagService {
  private final VectorStore vectorStore;

  /**
   * 질문을 기준으로 관련 문서를 검색하고
   * Gemini 프롬프트에 사용할 context 문자열로 반환한다.
   *
   * 처리 순서:
   * 1. 벡터 검색 topK 수행
   * 2. 문서 text 추출
   * 3. 하나의 context 문자열 생성
   *
   * @param question RAG 검색용 질문
   * @return 검색된 문서 context
   */
  public String searchContext(String question) {

    List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(question)
                    .topK(3) // 가장 유사한 3개 문서 chunk만 반환
                    .build()
    );

    if (docs == null || docs.isEmpty()) {
      return "";
    }

    String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));

    if (context.length() > 1000) {
      context = context.substring(0, 1000);
    }

    return context;
  }
}
