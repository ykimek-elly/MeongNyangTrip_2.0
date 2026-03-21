package com.team.meongnyang.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 카카오 Local API 교차검증 서비스.
 *
 * 1단계: 공공데이터 주소의 정확한 좌표 확보 (주소 → 위경도 변환)
 * 2단계: 장소 영업 여부 확인 (폐업 장소 필터링)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLocalVerifyService {

    private static final String KAKAO_LOCAL_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    /** 좌표 불일치 허용 오차 (미터). 이 이상이면 Kakao 좌표로 보정 */
    private static final double COORD_THRESHOLD_METERS = 100.0;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    private final RestClient restClient;

    /**
     * 교차검증 결과 레코드.
     * @param isActive  카카오 검색 결과 존재 여부 (false = 폐업/오류 → DB 저장 제외)
     * @param lat       검증 후 최종 위도 (Kakao 좌표 우선)
     * @param lng       검증 후 최종 경도 (Kakao 좌표 우선)
     * @param kakaoId   카카오 장소 ID (중복 dedup 키, API 오류 시 null)
     */
    public record VerifyResult(boolean isActive, double lat, double lng, String kakaoId, String placeUrl) {}

    /**
     * 장소명 + 공공데이터 좌표를 Kakao Local API로 교차검증한다.
     *
     * @param placeName  검증할 장소명
     * @param publicLat  공공데이터 위도
     * @param publicLng  공공데이터 경도
     * @return VerifyResult (isActive=false이면 배치에서 저장 생략)
     */
    public VerifyResult verify(String placeName, double publicLat, double publicLng) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(KAKAO_LOCAL_URL)
                    .queryParam("query", placeName)
                    .queryParam("x", publicLng)
                    .queryParam("y", publicLat)
                    .queryParam("radius", 500)
                    .queryParam("size", 1)
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return new VerifyResult(false, publicLat, publicLng, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

            if (documents == null || documents.isEmpty()) {
                // 2단계: 카카오 검색 결과 없음 → 폐업 또는 오류 장소
                log.debug("[검증실패-폐업] {}", placeName);
                return new VerifyResult(false, publicLat, publicLng, null, null);
            }

            Map<String, Object> top = documents.get(0);
            double kakaoLat = Double.parseDouble((String) top.get("y"));
            double kakaoLng = Double.parseDouble((String) top.get("x"));
            String kakaoName = (String) top.getOrDefault("place_name", "");
            String kakaoId = (String) top.get("id");
            String placeUrl = (String) top.get("place_url");

            // 이름 유사도 검증 — 다른 업체가 같은 위치에 있는 경우 차단
            if (!isNameSimilar(placeName, kakaoName)) {
                log.warn("[이름불일치-폐업의심] DB='{}' Kakao='{}' → 다른 업체로 판단",
                        placeName, kakaoName);
                return new VerifyResult(false, publicLat, publicLng, null, null);
            }

            // 좌표 오차 계산
            double distanceM = haversineMeters(publicLat, publicLng, kakaoLat, kakaoLng);

            if (distanceM <= COORD_THRESHOLD_METERS) {
                log.debug("[검증성공] {} (오차 {}m)", placeName, (int) distanceM);
                return new VerifyResult(true, publicLat, publicLng, kakaoId, placeUrl);
            } else {
                log.debug("[좌표보정] {} (오차 {}m → Kakao 좌표 사용)", placeName, (int) distanceM);
                return new VerifyResult(true, kakaoLat, kakaoLng, kakaoId, placeUrl);
            }

        } catch (Exception e) {
            log.warn("[검증오류] {} — {}", placeName, e.getMessage());
            // API 오류 시 공공데이터 좌표 그대로 사용 (검증 생략)
            return new VerifyResult(true, publicLat, publicLng, null, null);
        }
    }

    /**
     * 장소명 유사도 검증 — 공백/특수문자 제거 후 포함 관계 또는 공통 키워드(3자+) 여부 확인.
     * Kakao 검색 결과가 DB 장소명과 전혀 다른 업체인지 판별.
     */
    private boolean isNameSimilar(String dbName, String kakaoName) {
        if (dbName == null || kakaoName == null) return false;
        String n1 = dbName.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
        String n2 = kakaoName.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
        if (n1.contains(n2) || n2.contains(n1)) return true;
        // 3글자 이상 공통 부분 있는지 확인
        for (int len = 3; len <= n1.length(); len++) {
            for (int i = 0; i <= n1.length() - len; i++) {
                if (n2.contains(n1.substring(i, i + len))) return true;
            }
        }
        return false;
    }

    /** Haversine 공식으로 두 좌표 간 거리(미터) 계산 */
    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6_371_000; // 지구 반경 (미터)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
