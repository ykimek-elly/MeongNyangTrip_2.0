package com.team.meongnyang.recommendation.rag.service;

import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.recommendation.weather.dto.WeatherContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 반려견 추천용 RAG 검색을 담당한다.
 *
 * 역할:
 * - 검색용 질의를 생성한다.
 * - 벡터 검색 결과에서 추천 목적에 맞는 chunk만 필터링한다.
 * - 프롬프트에 넣기 좋은 짧은 context로 압축한다.
 */
/**
 * 추천 설명에 필요한 보조 문맥만 벡터 검색으로 가져오는 RAG 검색 서비스이다.
 *
 * <p>파이프라인 흐름에서 후보 장소 수집 뒤, 프롬프트 생성 전에 호출되며
 * 반려동물 특성과 날씨에 맞는 검색 질의를 만들고 추천에 직접 도움이 되는 문장만 압축한다.
 * 이렇게 만든 문맥은 프롬프트에 포함되어 AI 코멘트의 설명력을 보강한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RagService {

  // RAG 관련 상수
  private static final int QUERY_TOP_K = 2;
  private static final int MAX_CONTEXT_CHUNKS = 4;
  private static final int MAX_CONTEXT_LENGTH = 700;

  private static final List<String> POSITIVE_KEYWORDS = List.of(
          "산책", "주의", "주의사항", "건강", "건강관리", "운동", "야외활동", "외출", "활동량",
          "발바닥", "호흡", "체온", "더위", "추위", "비", "우천", "바람", "실내", "소형견",
          "견종", "말티즈", "목줄", "하네스", "수분", "휴식", "관절", "피부", "털", "보호자"
  );

  private static final List<String> NEGATIVE_KEYWORDS = List.of(
          "시장", "통계", "점유율", "사료", "브랜드", "매출", "판매", "소비", "지출", "연도별",
          "산업", "분석", "전망", "순위", "인기", "비용", "구매", "가격", "퍼센트", "보고서"
  );

  private final VectorStore vectorStore;

  /**
   * 반려견 정보와 날씨 정보를 바탕으로 RAG context를 생성한다.
   *
   * @param pet 반려견 정보
   * @param weather 현재 날씨 정보
   * @return 정제된 RAG context. 적절한 결과가 없으면 빈 문자열을 반환한다.
   */
  /**
   * 반려동물 정보와 현재 날씨를 바탕으로 추천 프롬프트에 넣을 RAG 문맥을 생성한다.
   *
   * @param pet 추천 기준이 되는 반려동물 정보
   * @param weather 현재 추천 시점의 날씨 문맥
   * @return 추천 문장 보강에 사용할 압축된 RAG 문맥, 적절한 결과가 없으면 빈 문자열
   */
  public String searchContext(Pet pet, WeatherContext weather) {
    // 1. 반려견 정보와 날씨를 바탕으로 검색용 질의를 만든다.
    List<String> queries = buildRagQueries(pet, weather);
    log.info("[RAG] 검색 시작 queryCount={}, queries={}", queries.size(), queries);
    // 2. 질의별 raw 검색 결과를 수집한다.
    List<Document> rawDocuments = new ArrayList<>();

    for (String query : queries) {
      // 2-1. 각 질의마다 작은 topK로 벡터 검색을 수행한다.
      List<Document> docs = vectorStore.similaritySearch(
              SearchRequest.builder()
                      .query(query)
                      .topK(QUERY_TOP_K)
                      .build()
      );

      int rawCount = docs == null ? 0 : docs.size();
      log.info("[RAG] 쿼리 검색 결과 query={}, rawResults={}", query, rawCount);

      if (docs != null) {
        rawDocuments.addAll(docs);
      }
    }

    // 3. 검색 결과가 없으면 빈 context를 반환한다.
    if (rawDocuments.isEmpty()) {
      log.info("[RAG] 최종 문맥 없음 usedQueries={}, rawTotal=0, filteredTotal=0, finalContext=''", queries);
      return "";
    }

    // 4. 추천 목적에 맞는 chunk만 필터링하고 짧게 압축한다.
    LinkedHashSet<String> filteredChunks = new LinkedHashSet<>();
    for (Document document : rawDocuments) {
      String text = document.getText();

      // 4-1. 산책/건강관리 관련 문맥만 남긴다.
      if (!isUsefulRagChunk(text)) {
        continue;
      }

      // 4-2. 프롬프트에 넣기 쉬운 짧은 문장으로 압축한다.
      String compressed = compressChunk(text);
      if (!compressed.isBlank()) {
        filteredChunks.add(compressed);
      }

      // 4-3. 최종 context 길이를 과도하게 키우지 않도록 개수를 제한한다.
      if (filteredChunks.size() >= MAX_CONTEXT_CHUNKS) {
        break;
      }
    }

    // 5. 최종 context 길이를 제한하고 로그와 함께 반환한다.
    String context = limitContextLength(String.join("\n", filteredChunks));
    log.info("[RAG] 검색 완료 contextLength={}, filteredChunks={}", context.length(), filteredChunks.size());
    log.info(
            "[RAG] 최종 문맥 요약 usedQueries={}, rawTotal={}, filteredTotal={}, finalContext={}",
            queries,
            rawDocuments.size(),
            filteredChunks.size(),
            context
    );
    return context;
  }

  /**
   * 검색 목적별로 짧은 RAG 질의를 만든다.
   *
   * @param pet 반려견 정보
   * @param weather 현재 날씨 정보
   * @return 중복이 제거된 검색 질의 목록
   */
  List<String> buildRagQueries(Pet pet, WeatherContext weather) {
    // 1. 견종명을 검색에 사용할 기본 키워드로 정리한다.
    String breed = safeText(pet != null ? pet.getPetBreed() : null, "반려견");
    // 2. 체급 정보를 일반화된 검색 키워드로 정리한다.
    String size = pet != null && pet.getPetSize() != null
            ? pet.getPetSize().name().toLowerCase(Locale.ROOT) + "견"
            : "반려견";
    // 3. 날씨 수치를 자연어 기반 검색 표현으로 변환한다.
    String weatherQuery = buildWeatherQuery(weather);

    // 4. 목적형 짧은 질의를 조합해 중복 없이 반환한다.
    LinkedHashSet<String> queries = new LinkedHashSet<>();
    queries.add(breed + " 산책 주의사항");
    queries.add(size + " 건강관리 운동 가이드");
    queries.add(breed + " " + weatherQuery + " 야외활동 주의사항");
    return new ArrayList<>(queries);
  }

  /**
   * 날씨 수치 정보를 검색 친화적인 자연어 표현으로 변환한다.
   *
   * @param weather 현재 날씨 정보
   * @return 예: "비 오는 날", "선선한 날", "더운 날", "바람이 강한 날"
   */
  String buildWeatherQuery(WeatherContext weather) {
    // 1. 날씨 정보가 없으면 중립적인 표현을 사용한다.
    if (weather == null) {
      return "무난한 날";
    }

    List<String> parts = new ArrayList<>();

    // 2. 강수/기온 상태를 검색 친화적인 문장으로 변환한다.
    if (weather.isRaining() || weather.getRainfall() > 0) {
      parts.add("비 오는 날");
    } else if (weather.isHot()) {
      parts.add("더운 날");
    } else if (weather.isCold()) {
      parts.add("쌀쌀한 날");
    } else if (weather.getTemperature() <= 18.0) {
      parts.add("선선한 날");
    } else {
      parts.add("무난한 날");
    }

    // 3. 바람이 강하면 별도 조건을 추가한다.
    if (weather.isWindy()) {
      parts.add("바람이 강한 날");
    }

    // 4. 검색 질의에 바로 붙일 수 있는 형태로 결합한다.
    return String.join(" ", parts);
  }

  /**
   * chunk가 추천 목적에 유용한 문맥인지 판별한다.
   *
   * <p>산책, 건강관리, 야외활동 관련 키워드는 포함하고
   * 시장, 통계, 사료, 점유율 등 노이즈 문맥은 제외한다.
   *
   * @param chunk 검색된 원문 chunk
   * @return 유용한 chunk이면 true
   */
  boolean isUsefulRagChunk(String chunk) {
    if (chunk == null || chunk.isBlank()) {
      return false;
    }

    String normalized = normalize(chunk);
    boolean hasPositive = POSITIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    boolean hasNegative = NEGATIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    return hasPositive && !hasNegative;
  }

  /**
   * 긴 chunk를 프롬프트에 넣기 쉬운 짧은 문장으로 압축한다.
   *
   * @param chunk 검색된 원문 chunk
   * @return 정제된 요약 문장. 적절한 문장이 없으면 빈 문자열을 반환한다.
   */
  String compressChunk(String chunk) {
    // 1. 빈 chunk는 바로 제외한다.
    if (chunk == null || chunk.isBlank()) {
      return "";
    }

    // 2. 문장 단위로 분리한다.
    // 3. 유용한 문장만 남긴다.
    // 4. 숫자성 잡음을 제거한다.
    // 5. 1~2문장으로 짧게 결합한다.
    return Arrays.stream(chunk.split("(?<=[.!?。]|다\\.)\\s+|\\n+"))
            .map(String::trim)
            .filter(sentence -> !sentence.isBlank())
            .filter(this::isUsefulSentence)
            .map(this::stripNoise)
            .filter(sentence -> !sentence.isBlank())
            .limit(2)
            .reduce((left, right) -> left + " " + right)
            .map(text -> text.length() > 180 ? text.substring(0, 180) + "..." : text)
            .orElse("");
  }

  /**
   * 개별 문장이 추천 목적에 맞는지 판별한다.
   *
   * @param sentence 검사 대상 문장
   * @return 활용 가능한 문장이면 true
   */
  private boolean isUsefulSentence(String sentence) {
    String normalized = normalize(sentence);
    boolean hasPositive = POSITIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    boolean hasNegative = NEGATIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    return hasPositive && !hasNegative;
  }

  /**
   * 숫자성 잡음을 제거해 문장을 더 간결하게 만든다.
   *
   * @param sentence 원본 문장
   * @return 불필요한 수치 표현이 제거된 문장
   */
  private String stripNoise(String sentence) {
    String normalized = sentence.replaceAll("\\s+", " ").trim();
    return normalized.replaceAll("([0-9]{4}년|[0-9]+\\.?[0-9]*%|[0-9]+\\.?[0-9]*원)", "").trim();
  }

  /**
   * 최종 context 길이를 제한한다.
   *
   * @param context 압축된 context
   * @return 최대 길이 제한이 적용된 context
   */
  private String limitContextLength(String context) {
    if (context == null || context.isBlank()) {
      return "";
    }
    return context.length() > MAX_CONTEXT_LENGTH
            ? context.substring(0, MAX_CONTEXT_LENGTH) + "..."
            : context;
  }

  /**
   * 키워드 비교를 위해 공백과 대소문자를 정규화한다.
   *
   * @param text 원본 문자열
   * @return 정규화된 문자열
   */
  private String normalize(String text) {
    return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  /**
   * 문자열이 비어 있으면 대체값을 반환한다.
   *
   * @param value 원본 문자열
   * @param fallback 대체 문자열
   * @return 정리된 문자열
   */
  private String safeText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
