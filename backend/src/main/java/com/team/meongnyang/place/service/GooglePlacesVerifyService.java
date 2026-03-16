package com.team.meongnyang.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Places API (New) 속성 보강 서비스 — API 명세 3단계.
 *
 * Text Search로 시설명+주소 검색 → allowsDogs 필드 취득.
 * Kakao 교차검증이 없는 한국문화정보원 데이터(isVerified=false)에 적용.
 *
 * 엔드포인트: POST https://places.googleapis.com/v1/places:searchText
 * 필드마스크: places.allowsDogs, places.businessStatus, places.location
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlacesVerifyService {

    private static final String PLACES_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String FIELD_MASK =
            "places.displayName,places.allowsDogs,places.businessStatus,places.location,places.rating,places.userRatingCount";

    @Value("${google.places.api-key:}")
    private String apiKey;

    private final RestClient restClient;

    /**
     * 보강 결과 레코드.
     * @param allowsDogs      Google이 반려동물 동반 가능으로 확인한 경우 true
     * @param isClosed        영구 폐업 확인된 경우 true
     * @param googleRating    Google Places 별점 (없으면 null)
     * @param googleReviewCount Google 리뷰 수 (없으면 null)
     */
    public record EnrichResult(boolean allowsDogs, boolean isClosed,
                               Double googleRating, Integer googleReviewCount) {}

    /**
     * 시설명 + 주소로 Google Places 검색 후 allowsDogs / 폐업 여부 반환.
     * API 키 미설정 또는 오류 시 (false, false) 반환 — 배치 중단 없음.
     */
    @SuppressWarnings({"unchecked", "null"})
    public EnrichResult enrich(String name, String address) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Google Places] API 키 미설정 — 보강 건너뜀");
            return new EnrichResult(false, false, null, null);
        }

        try {
            String query = (address != null)
                    ? name + " " + address
                    : name;

            Map<String, Object> body = Map.of(
                    "textQuery", query.trim(),
                    "languageCode", "ko",
                    "maxResultCount", 1
            );

            Map<String, Object> response = restClient.post()
                    .uri(PLACES_URL)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return new EnrichResult(false, false, null, null);

            List<Map<String, Object>> places =
                    (List<Map<String, Object>>) response.get("places");
            if (places == null || places.isEmpty()) return new EnrichResult(false, false, null, null);

            Map<String, Object> top = places.get(0);
            Boolean allowsDogs = (Boolean) top.get("allowsDogs");
            String status = (String) top.get("businessStatus");

            // 별점 및 리뷰 수 추출
            Double rating = top.get("rating") instanceof Number n ? n.doubleValue() : null;
            Integer reviewCount = top.get("userRatingCount") instanceof Number n ? n.intValue() : null;

            boolean closed = "CLOSED_PERMANENTLY".equals(status);
            boolean petOk = Boolean.TRUE.equals(allowsDogs);

            log.debug("[Google] {} → allowsDogs={}, status={}, rating={}", name, petOk, status, rating);
            return new EnrichResult(petOk, closed, rating, reviewCount);

        } catch (Exception e) {
            log.warn("[Google검증오류] {} — {}", name, e.getMessage());
            return new EnrichResult(false, false, null, null);
        }
    }
}
