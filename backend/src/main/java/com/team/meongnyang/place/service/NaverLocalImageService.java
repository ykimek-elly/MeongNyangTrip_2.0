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
     * PlaceEnrichBatchService에서 Google 보강과 함께 사용.
     *
     * @param placeName 장소명
     * @return isActive: 네이버에서 검색된 경우 true, imageUrl: 취득한 이미지 URL (없으면 null)
     */
    public VerifyResult verifyAndFetchImage(String placeName) {
        if (!enabled) return new VerifyResult(true, null); // 비활성화 시 운영중으로 가정

        try {
            Map<String, Object> response = callLocalSearch(placeName);
            if (response == null) return new VerifyResult(false, fetchFromImageSearch(placeName));

            Integer total = (Integer) response.get("total");
            boolean isActive = total != null && total > 0;

            String imageUrl = null;
            if (isActive) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null && !items.isEmpty()) {
                    String thumbnail = (String) items.get(0).get("thumbnail");
                    imageUrl = (thumbnail != null && !thumbnail.isBlank()) ? thumbnail : null;
                }
            }

            // thumbnail 없으면 이미지 검색 fallback
            if (imageUrl == null) {
                imageUrl = fetchFromImageSearch(placeName);
            }

            return new VerifyResult(isActive, imageUrl);

        } catch (Exception e) {
            log.warn("[NaverVerify] 조회 실패: name='{}', error={}", placeName, e.getMessage());
            return new VerifyResult(true, null); // 오류 시 운영중으로 가정 (보수적 처리)
        }
    }

    /**
     * 이미지만 조회 — NaverImageEnrichBatchService (imageUrl IS NULL 대상) 전용.
     */
    public String fetchThumbnailUrl(String placeName) {
        if (!enabled) return null;
        try {
            Map<String, Object> response = callLocalSearch(placeName);
            if (response == null) return fetchFromImageSearch(placeName);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null && !items.isEmpty()) {
                String thumbnail = (String) items.get(0).get("thumbnail");
                if (thumbnail != null && !thumbnail.isBlank()) return thumbnail;
            }
        } catch (Exception e) {
            log.warn("[NaverImage] 지역검색 실패: name='{}', error={}", placeName, e.getMessage());
        }
        return fetchFromImageSearch(placeName);
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

    @SuppressWarnings("unchecked")
    private String fetchFromImageSearch(String placeName) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/image.json")
                            .queryParam("query", placeName)
                            .queryParam("display", 1)
                            .queryParam("filter", "large")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) return null;

            String link = (String) items.get(0).get("link");
            return (link != null && !link.isBlank()) ? link : null;

        } catch (Exception e) {
            log.warn("[NaverImage] 이미지검색 실패: name='{}', error={}", placeName, e.getMessage());
            return null;
        }
    }
}
