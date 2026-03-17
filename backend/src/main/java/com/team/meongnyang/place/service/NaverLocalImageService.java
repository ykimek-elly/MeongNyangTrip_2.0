package com.team.meongnyang.place.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 네이버 검색 API — 장소 운영 교차검증 + 대표 이미지 조회 서비스.
 *
 * [통합 검증 - verifyAndFetchImage()]
 *   1차: 지역 검색 → isActive(운영여부) + thumbnail(업체 등록 사진)
 *   2차: 이미지 검색 → imageUrl fallback (thumbnail 없을 때)
 *
 * [이미지만 - fetchThumbnailUrl()]
 *   imageUrl IS NULL 대상 단독 이미지 보강용
 *
 * API 키 미설정 시 자동 비활성화(null/기본값 반환).
 */
@Slf4j
@Service
public class NaverLocalImageService {

    /** 운영 여부 + 이미지 URL 복합 결과 */
    public record VerifyResult(boolean isActive, String imageUrl) {}

    /** 블로그 분석 결과 — 화제성 + 최신성 + 감성 데이터 */
    public record BlogResult(int total, String latestPostDate, List<String> descriptions) {
        public static BlogResult empty() {
            return new BlogResult(0, null, List.of());
        }
    }

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final boolean enabled;

    public NaverLocalImageService(
            @Value("${naver.local.client-id:}") String clientId,
            @Value("${naver.local.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.enabled = !clientId.isBlank() && !clientSecret.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl("https://openapi.naver.com/v1/search")
                .build();
        if (!this.enabled) {
            log.warn("[NaverImage] API 키 미설정 — 이미지/검증 비활성화. 환경변수 확인 필요.");
        }
    }

    /**
     * 운영 여부 교차검증 + 이미지 조회 통합 메서드.
     * 지역명을 쿼리에 포함시켜 동명이업체 혼동 방지.
     *
     * @param placeName 장소명
     * @param address   공공데이터 주소 (시/군 추출용) — null 허용
     * @return isActive: 네이버에서 검색된 경우 true, imageUrl: 취득한 이미지 URL (없으면 null)
     */
    public VerifyResult verifyAndFetchImage(String placeName, String address) {
        if (!enabled) return new VerifyResult(true, null); // 비활성화 시 운영중으로 가정

        String regionHint = extractRegion(address);
        String query = (regionHint != null) ? placeName + " " + regionHint : placeName;

        try {
            Map<String, Object> response = callLocalSearch(query);
            if (response == null) return new VerifyResult(false, null);

            Integer total = (Integer) response.get("total");
            boolean isActive = total != null && total > 0;

            String imageUrl = null;
            if (isActive) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null && !items.isEmpty()) {
                    // 이름 유사도 검증 — HTML 태그 제거 후 비교
                    String naverTitle = ((String) items.get(0).getOrDefault("title", ""))
                            .replaceAll("<[^>]+>", "");
                    if (!isNameSimilar(placeName, naverTitle)) {
                        log.warn("[네이버이름불일치] DB='{}' Naver='{}' → 다른 업체 추정, 폐업의심 처리",
                                placeName, naverTitle);
                        return new VerifyResult(false, null);
                    }
                    String thumbnail = (String) items.get(0).get("thumbnail");
                    imageUrl = (thumbnail != null && !thumbnail.isBlank()) ? thumbnail : null;
                }
            }

            return new VerifyResult(isActive, imageUrl);

        } catch (Exception e) {
            log.warn("[NaverVerify] 조회 실패: name='{}', error={}", placeName, e.getMessage());
            return new VerifyResult(true, null); // 오류 시 운영중으로 가정 (보수적 처리)
        }
    }

    /**
     * 이미지만 조회 — NaverImageEnrichBatchService (imageUrl IS NULL 대상) 전용.
     * 네이버 이미지 검색 API(/v1/search/image) 사용 (Local Search는 thumbnail 미반환).
     *
     * filter=cafeblog: 블로그/카페 이미지만 반환 → 뉴스 사진(imgnews.naver.net) 원천 차단
     * display=5: 상위 5건 중 뉴스CDN 제외 첫 번째 결과 선택
     */
    @SuppressWarnings("unchecked")
    public String fetchThumbnailUrl(String placeName) {
        if (!enabled) return null;
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/image")
                            .queryParam("query", placeName)
                            .queryParam("display", 5)
                            .queryParam("filter", "cafeblog")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    String thumbnail = (String) item.get("thumbnail");
                    if (thumbnail != null && !thumbnail.isBlank()
                            && !thumbnail.contains("imgnews.naver.net")) {
                        return thumbnail;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[NaverImage] 이미지검색 실패: name='{}', error={}", placeName, e.getMessage());
        }
        return null;
    }

    /**
     * 네이버 블로그 검색 — 화제성(total) + 최신성(postdate) + 감성(descriptions) 추출.
     * 운영 중으로 확정된 장소 대상으로만 호출 (폐업 장소 제외).
     *
     * @param placeName 장소명
     * @return BlogResult (API 비활성화 또는 오류 시 empty 반환)
     */
    @SuppressWarnings("unchecked")
    public BlogResult fetchBlogData(String placeName) {
        if (!enabled) return BlogResult.empty();
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/blog.json")
                            .queryParam("query", placeName + " 애견동반")
                            .queryParam("display", 5)
                            .queryParam("sort", "date")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return BlogResult.empty();

            int total = response.get("total") instanceof Integer t ? t : 0;
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            String latestPostDate = null;
            List<String> descriptions = List.of();

            if (items != null && !items.isEmpty()) {
                latestPostDate = (String) items.get(0).get("postdate");
                descriptions = items.stream()
                        .map(item -> (String) item.getOrDefault("description", ""))
                        .filter(d -> d != null && !d.isBlank())
                        .toList();
            }
            return new BlogResult(total, latestPostDate, descriptions);

        } catch (Exception e) {
            log.warn("[NaverBlog] 조회 실패: name='{}', error={}", placeName, e.getMessage());
            return BlogResult.empty();
        }
    }

    /**
     * 주소에서 시/군/구 단위 지역명 추출.
     * "경기도 파주시 책향기로 573" → "파주시"
     * "서울특별시 성동구 뚝섬로 273" → "성동구"
     */
    private String extractRegion(String address) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.trim().split("\\s+");
        // 광역시/도 다음 토큰(시/군/구)을 반환
        if (parts.length >= 2) return parts[1];
        return null;
    }

    /** 장소명 유사도 검증 — 공백/특수문자 제거 후 포함 관계 또는 3자+ 공통 키워드 여부 확인 */
    private boolean isNameSimilar(String dbName, String searchName) {
        if (dbName == null || searchName == null) return false;
        String n1 = dbName.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
        String n2 = searchName.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
        if (n1.contains(n2) || n2.contains(n1)) return true;
        for (int len = 3; len <= n1.length(); len++) {
            for (int i = 0; i <= n1.length() - len; i++) {
                if (n2.contains(n1.substring(i, i + len))) return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callLocalSearch(String placeName) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/local.json")
                        .queryParam("query", placeName)
                        .queryParam("display", 1)
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .body(Map.class);
    }

}
