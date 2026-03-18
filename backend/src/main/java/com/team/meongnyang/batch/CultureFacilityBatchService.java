package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.PlaceVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 한국문화정보원 반려동물 동반 가능 문화시설 배치 서비스 (서울+경기 한정).
 * 데이터셋 ID: 15111389
 *
 * [파이프라인 2.0 — 검증 후 저장]
 *   1단계: odcloud.kr API 수집
 *   2단계: PlaceVerificationService(네이버+카카오) 이중 교차검증
 *   3단계: 검증 통과 시에만 DB 저장 (isVerified=true)
 *   4단계: kakaoId 기준 데이터셋 간 중복 방지
 *
 * 수집 대상 (카테고리2 기준):
 *   반려동반여행 → PLACE (박물관/여행지/미술관/문예회관), STAY (펜션)
 *   반려동물식당카페 → DINING
 *   반려문화시설 → PLACE
 *   반려의료, 반려동물 서비스 → 제외 (동물병원 탭 별도 처리)
 *
 * contentId: "KCISA" + 시설명+주소 해시 (배치 Upsert 기준키)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CultureFacilityBatchService {

    private static final String API_URL =
            "https://api.odcloud.kr/api/15111389/v1/uddi:41944402-8249-4e45-9e9d-a52d0a7db1cc";
    private static final int PAGE_SIZE = 1000;

    private final PlaceRepository placeRepository;
    private final PlaceVerificationService placeVerificationService;
    private final RestClient restClient;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${pet-tour.service-key}")
    private String serviceKey;

    /**
     * 문화시설 배치 실행.
     * 수동 실행: POST /api/v1/admin/batch/culture
     */
    @Transactional
    @CacheEvict(value = "places", allEntries = true)
    public void runCultureBatch() {
        log.info("===== 한국문화정보원 문화시설 배치 시작 (네이버+카카오 이중검증) =====");
        int totalSaved = 0;
        int totalSkipped = 0;
        int totalDuplicated = 0;
        int page = 1;

        while (true) {
            List<Map<String, Object>> items = fetchPage(page);
            if (items == null || items.isEmpty()) break;

            for (Map<String, Object> item : items) {
                try {
                    int result = processItem(item);
                    if (result == 1)       totalSaved++;
                    else if (result == -1) totalDuplicated++;
                    else                   totalSkipped++;
                } catch (Exception e) {
                    log.error("[문화시설 처리오류] 시설명={} — {}", item.get("시설명"), e.getMessage());
                    totalSkipped++;
                }
                // Naver+Kakao API 속도 제한 (각 10 req/s) 준수
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }

            log.info("[문화시설] 페이지 {} 처리 완료 (저장: {}건 / 중복생략: {}건 / 제외: {}건)",
                    page, totalSaved, totalDuplicated, totalSkipped);
            if (items.size() < PAGE_SIZE) break;
            page++;
        }

        log.info("===== 문화시설 배치 종료: 저장 {}건 / 중복 {}건 / 제외 {}건 =====",
                totalSaved, totalDuplicated, totalSkipped);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchPage(int page) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(API_URL)
                    .queryParam("page", page)
                    .queryParam("perPage", PAGE_SIZE)
                    .queryParam("returnType", "JSON")
                    .queryParam("serviceKey", serviceKey)
                    .build(true)
                    .toUri();

            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return List.of();
            return (List<Map<String, Object>>) response.get("data");
        } catch (Exception e) {
            log.error("[문화시설 API 오류] page={} — {}", page, e.getMessage());
            return List.of();
        }
    }

    /**
     * 단건 처리: 수집 → 교차검증 → 중복체크 → Upsert
     *
     * @return 1=저장, -1=중복생략, 0=건너뜀(검증실패/오류)
     */
    @SuppressWarnings("null")
    private int processItem(Map<String, Object> item) {
        String cat2 = str(item, "카테고리2");
        String cat3 = str(item, "카테고리3");
        String category = mapCategory(cat2, cat3);
        if (category == null) return 0;

        String name = str(item, "시설명");
        String address = str(item, "도로명주소");
        String latStr = str(item, "위도");
        String lngStr = str(item, "경도");

        if (name == null || address == null || latStr == null || lngStr == null) return 0;

        // 서울·경기 외 지역 제외
        if (!address.startsWith("서울") && !address.startsWith("경기")) return 0;

        double lat, lng;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            return 0;
        }
        if (lat == 0.0 || lng == 0.0) return 0;

        // ─── 2단계: 네이버+카카오 이중 교차검증 ───────────────────────────────
        PlaceVerificationService.VerificationResult verify =
                placeVerificationService.verify(name, address, lat, lng);

        if (!verify.confirmed()) {
            log.debug("[문화시설 검증실패-폐기] '{}' ({})", name, verify.source());
            return 0;
        }

        // ─── 중복 체크: kakaoId 기준 (KTO + KCISA 교차 dedup) ─────────────────
        if (verify.kakaoId() != null
                && placeRepository.findByKakaoId(verify.kakaoId()).isPresent()) {
            log.debug("[문화시설 중복-생략] '{}' — kakaoId={}", name, verify.kakaoId());
            return -1;
        }

        // ─── 3단계: 확정 좌표·이미지 적용 ────────────────────────────────────
        double finalLat = verify.lat();
        double finalLng = verify.lng();
        Point geom = geometryFactory.createPoint(new Coordinate(finalLng, finalLat));

        // 이미지: Naver 취득 우선, 없으면 null (이미지 보강 배치에서 보완)
        String imageUrl = verify.imageUrl();
        String phone    = str(item, "전화번호");
        String homepage = cleanHomepage(str(item, "홈페이지"));
        String tags     = cat3 != null ? cat3 : cat2;

        // contentId: 시설명+주소 해시 (KCISA + 8자리 hex)
        int hash = (name + "|" + address).hashCode();
        String contentId = "KCISA" + String.format("%08x", hash & 0xFFFFFFFFL);

        final String fc = category, fp = phone, fh = homepage, ft = tags;
        final String fKakaoId = verify.kakaoId();
        final String fImageUrl = imageUrl;
        final double fLat = finalLat, fLng = finalLng;
        final Point  fGeom = geom;
        final String fSource = verify.source();

        placeRepository.findByContentId(contentId).ifPresentOrElse(
            existing -> existing.upsertFromBatch(
                name, address, null, fLat, fLng, fGeom, fc, fImageUrl, fp,
                null, fh, null, null, null, true, fKakaoId
            ),
            () -> {
                Place newPlace = Place.builder()
                        .contentId(contentId)
                        .kakaoId(fKakaoId)
                        .title(name)
                        .address(address)
                        .latitude(fLat)
                        .longitude(fLng)
                        .geom(fGeom)
                        .category(fc)
                        .imageUrl(fImageUrl)
                        .phone(fp)
                        .homepage(fh)
                        .tags(ft + (!"BOTH".equals(fSource) ? "[" + fSource + "]" : ""))
                        .isVerified(true)
                        .build();
                placeRepository.save(newPlace);
                log.debug("[문화시설 저장] '{}' source={} kakaoId={}", name, fSource, fKakaoId);
            }
        );
        return 1;
    }

    /** 카테고리2 + 카테고리3 → Place.category 매핑 */
    private String mapCategory(String cat2, String cat3) {
        if (cat2 == null) return null;
        return switch (cat2) {
            case "반려동반여행" -> "펜션".equals(cat3) ? "STAY" : "PLACE";
            case "반려동물식당카페" -> "DINING";
            case "반려문화시설" -> "PLACE";
            default -> null; // 반려의료, 반려동물 서비스 → 제외
        };
    }

    /** "정보없음" / 빈 값 → null 처리 */
    private String cleanHomepage(String s) {
        if (s == null || s.isBlank() || "정보없음".equals(s)) return null;
        return s;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
