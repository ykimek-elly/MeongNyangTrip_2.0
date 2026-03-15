package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
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
 * 한국문화정보원 전국 반려동물 동반 가능 문화시설 배치 서비스.
 * 데이터셋 ID: 15111389
 *
 * 수집 대상 (카테고리2 기준):
 *   반려동반여행 → PLACE (박물관/여행지/미술관/문예회관), STAY (펜션)
 *   반려동물식당카페 → DINING
 *   반려문화시설 → PLACE
 *   반려의료, 반려동물 서비스 → 제외 (동물병원 탭 별도 처리)
 *
 * contentId: "KCISA" + 시설명+주소 해시 (배치 Upsert 기준키)
 * 좌표: API에서 WGS84(위도/경도) 직접 제공 → Kakao 교차검증 불필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CultureFacilityBatchService {

    private static final String API_URL =
            "https://api.odcloud.kr/api/15111389/v1/uddi:41944402-8249-4e45-9e9d-a52d0a7db1cc";
    private static final int PAGE_SIZE = 1000;

    private final PlaceRepository placeRepository;
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
        log.info("===== 한국문화정보원 문화시설 배치 시작 =====");
        int totalSaved = 0;
        int totalSkipped = 0;
        int page = 1;

        while (true) {
            List<Map<String, Object>> items = fetchPage(page);
            if (items == null || items.isEmpty()) break;

            for (Map<String, Object> item : items) {
                try {
                    if (processItem(item)) totalSaved++;
                    else totalSkipped++;
                } catch (Exception e) {
                    log.error("[문화시설 처리오류] 시설명={} — {}", item.get("시설명"), e.getMessage());
                    totalSkipped++;
                }
            }

            log.info("[문화시설] 페이지 {} 처리 완료 (저장 누계: {}건)", page, totalSaved);
            if (items.size() < PAGE_SIZE) break;
            page++;
        }

        log.info("===== 문화시설 배치 종료: 저장 {}건 / 제외 {}건 =====", totalSaved, totalSkipped);
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

    @SuppressWarnings("null")
    private boolean processItem(Map<String, Object> item) {
        String cat2 = str(item, "카테고리2");
        String cat3 = str(item, "카테고리3");
        String category = mapCategory(cat2, cat3);
        if (category == null) return false; // 수집 대상 외 카테고리

        String name = str(item, "시설명");
        String address = str(item, "도로명주소");
        String latStr = str(item, "위도");
        String lngStr = str(item, "경도");

        if (name == null || address == null || latStr == null || lngStr == null) return false;

        double lat, lng;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            return false;
        }
        if (lat == 0.0 || lng == 0.0) return false;

        // contentId: 시설명+주소 해시 (KCISA + 8자리 hex)
        int hash = (name + "|" + address).hashCode();
        String contentId = "KCISA" + String.format("%08x", hash & 0xFFFFFFFFL);

        Point geom = geometryFactory.createPoint(new Coordinate(lng, lat));
        String phone = str(item, "전화번호");
        String homepage = cleanHomepage(str(item, "홈페이지"));
        String tags = cat3 != null ? cat3 : cat2;

        final String fc = category, fp = phone, fh = homepage, ft = tags;

        placeRepository.findByContentId(contentId).ifPresentOrElse(
            existing -> existing.upsertFromBatch(
                name, address, null, lat, lng, geom, fc, null, fp,
                null, fh, null, null, null, false
            ),
            () -> {
                Place newPlace = Place.builder()
                        .contentId(contentId)
                        .title(name)
                        .address(address)
                        .latitude(lat)
                        .longitude(lng)
                        .geom(geom)
                        .category(fc)
                        .phone(fp)
                        .homepage(fh)
                        .tags(ft)
                        .isVerified(false)
                        .build();
                placeRepository.save(newPlace);
            }
        );
        return true;
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
