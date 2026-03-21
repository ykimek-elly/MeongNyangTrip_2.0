package com.team.meongnyang.place.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 네이버 검색 API — 장소 운영 여부(이름/주소) 교차검증 서비스 (이미지 조회 제외).
 *
 * [검증 - verify()]
 *   지역 검색 → isActive(운영여부) 판단 (검색 결과 존재 여부 + 이름 유사도)
 *
 * [블로그 분석 - fetchBlogData()]
 *   화제성 + 최신성 + 감성 데이터 추출용
 *
 * API 키 미설정 시 자동 비활성화(null/기본값 반환).
 */
@Slf4j
@Service
public class NaverLocalVerifyService {

    /** 운영 여부 검증 결과 */
    public record VerifyResult(boolean isActive) {}

    /** 운영 여부 검증 결과 + Naver 매칭 제목 (숫자 접미사 false positive 탐지용) */
    public record TitledVerifyResult(boolean isActive, String naverTitle) {}

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

    public NaverLocalVerifyService(
            @Value("${naver.local.client-id:}") String clientId,
            @Value("${naver.local.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.enabled = !clientId.isBlank() && !clientSecret.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl("https://openapi.naver.com/v1/search")
                .build();
        if (!this.enabled) {
            log.warn("[NaverVerify] API 키 미설정 — 검증 비활성화. 환경변수 확인 필요.");
        }
    }

    /**
     * 운영 여부 교차검증 메서드.
     * 지역명을 쿼리에 포함시켜 동명이업체 혼동 방지.
     *
     * @param placeName 장소명
     * @param address   공공데이터 주소 (시/군 추출용) — null 허용
     * @return isActive: 네이버에서 검색된 경우 true
     */
    public VerifyResult verify(String placeName, String address) {
        if (!enabled) return new VerifyResult(true);

        String regionHint = extractRegion(address);
        String query = (regionHint != null) ? placeName + " " + regionHint : placeName;

        try {
            Map<String, Object> response = callLocalSearch(query);
            if (response == null) return new VerifyResult(false);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            if (items == null || items.isEmpty()) {
                log.debug("[네이버미검색-폐업의심] '{}' total={}, items=0", placeName,
                        response.get("total"));
                return new VerifyResult(false);
            }

            Map<String, Object> top = items.get(0);

            // 1. 이름 유사도 검증 — HTML 태그 제거 후 비교
            String naverTitle = ((String) top.getOrDefault("title", "")).replaceAll("<[^>]+>", "");
            if (!isNameSimilar(placeName, naverTitle)) {
                log.warn("[네이버이름불일치] DB='{}' Naver='{}' → 다른 업체 추정, 폐업의심 처리",
                        placeName, naverTitle);
                return new VerifyResult(false);
            }

            // 2. 주소 교차검증 — 구/군 단위로 일치 확인 (동명이업체 오탐 방지)
            String naverAddress = getNaverAddress(top);
            if (!isDistrictMatch(address, naverAddress)) {
                log.warn("[네이버주소불일치] DB='{}' 주소={} | Naver주소={} → 타 지역 동명업체 추정",
                        placeName, address, naverAddress);
                return new VerifyResult(false);
            }

            return new VerifyResult(true);

        } catch (Exception e) {
            log.warn("[NaverVerify] 조회 실패: name='{}', error={}", placeName, e.getMessage());
            return new VerifyResult(true);
        }
    }

    /**
     * 운영 여부 검증 + Naver 매칭 제목 반환 — 숫자 접미사 false positive 탐지용.
     * verify()와 로직 동일하되 매칭된 Naver 제목도 함께 반환한다.
     */
    public TitledVerifyResult verifyWithTitle(String placeName, String address) {
        if (!enabled) return new TitledVerifyResult(true, null);

        String regionHint = extractRegion(address);
        String query = (regionHint != null) ? placeName + " " + regionHint : placeName;

        try {
            Map<String, Object> response = callLocalSearch(query);
            if (response == null) return new TitledVerifyResult(false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            if (items == null || items.isEmpty()) {
                return new TitledVerifyResult(false, null);
            }

            Map<String, Object> top = items.get(0);
            String naverTitle = ((String) top.getOrDefault("title", "")).replaceAll("<[^>]+>", "");

            if (!isNameSimilar(placeName, naverTitle)) {
                return new TitledVerifyResult(false, naverTitle);
            }

            String naverAddress = getNaverAddress(top);
            if (!isDistrictMatch(address, naverAddress)) {
                return new TitledVerifyResult(false, naverTitle);
            }

            return new TitledVerifyResult(true, naverTitle);

        } catch (Exception e) {
            log.warn("[NaverVerify] 조회 실패: name='{}', error={}", placeName, e.getMessage());
            return new TitledVerifyResult(true, null);
        }
    }

    /**
     * 네이버 블로그 검색 — 화제성(total) + 최신성(postdate) + 감성(descriptions) 추출.
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

    private String extractRegion(String address) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.trim().split("\\s+");
        if (parts.length >= 2) return parts[1];
        return null;
    }

    private boolean isNameSimilar(String dbName, String searchName) {
        if (dbName == null || searchName == null) return false;
        String n1 = dbName.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
        String n2 = searchName.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();

        // 완전 일치 또는 포함 관계
        if (n1.equals(n2) || n1.contains(n2) || n2.contains(n1)) return true;

        // 최장 공통 연속 부분문자열(LCS) 비율 80% 이상
        int lcs = longestCommonSubstringLength(n1, n2);
        double ratio = (double) lcs / Math.max(n1.length(), n2.length());
        return ratio >= 0.8;
    }

    /** 최장 공통 연속 부분문자열 길이 계산 */
    private int longestCommonSubstringLength(String s1, String s2) {
        int max = 0;
        for (int i = 0; i < s1.length(); i++) {
            for (int j = 0; j < s2.length(); j++) {
                int len = 0;
                while (i + len < s1.length() && j + len < s2.length()
                        && s1.charAt(i + len) == s2.charAt(j + len)) {
                    len++;
                }
                if (len > max) max = len;
            }
        }
        return max;
    }

    /**
     * Naver 검색 결과 주소 추출 — roadAddress 우선, 없으면 address 사용.
     */
    private String getNaverAddress(Map<String, Object> item) {
        String road = (String) item.get("roadAddress");
        if (road != null && !road.isBlank()) return road;
        return (String) item.getOrDefault("address", "");
    }

    /**
     * 구/군 단위 주소 일치 검증.
     * 입력 주소에서 "OO구" 또는 "OO군"을 추출해 Naver 주소와 비교.
     * 주소 정보 없을 경우 통과(true) 처리.
     */
    private boolean isDistrictMatch(String inputAddress, String naverAddress) {
        if (inputAddress == null || inputAddress.isBlank()) return true;
        if (naverAddress == null || naverAddress.isBlank()) return true;

        // "OO구" 또는 "OO군" 추출
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\S+[구군])")
                .matcher(inputAddress);
        if (!m.find()) return true; // 구/군 없으면 시 단위만 확인

        String district = m.group(1);
        if (naverAddress.contains(district)) return true;

        // 시 단위 일치라도 허용 (서울특별시 ↔ 서울)
        String[] parts = inputAddress.trim().split("\\s+");
        if (parts.length > 0) {
            String city = parts[0].replaceAll("특별시|광역시|특별자치시|도$", "");
            return naverAddress.contains(city);
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
