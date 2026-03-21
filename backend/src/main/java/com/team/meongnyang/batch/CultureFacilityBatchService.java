package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.PlaceVerificationService;
import com.team.meongnyang.place.service.KakaoImageScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한국문화정보원 문화시설 배치 서비스.
 *
 * [파이프라인 4.0 — 지역+카테고리 필터 쿼리]
 *   전체 70,650건 순회 → 서울/경기 × 3카테고리 필터 쿼리로 ~1,086건만 수집 (65배 빠름)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CultureFacilityBatchService {

    private static final String API_URL = "https://api.odcloud.kr/api/15111389/v1/uddi:41944402-8249-4e45-9e9d-a52d0a7db1cc";
    private static final int PAGE_SIZE = 1000;

    // API 필터 파라미터 키 (URL 인코딩된 상태)
    // cond[시도 명칭::EQ]
    private static final String FILTER_REGION_KEY  = "cond%5B%EC%8B%9C%EB%8F%84%20%EB%AA%85%EC%B9%AD%3A%3AEQ%5D";
    // cond[카테고리2::EQ]
    private static final String FILTER_CATEGORY_KEY = "cond%5B%EC%B9%B4%ED%85%8C%EA%B3%A0%EB%A6%AC2%3A%3AEQ%5D";

    private static final String[] REGIONS     = {"서울특별시", "경기도"};
    private static final String[] CATEGORIES  = {"반려동반여행", "반려동물식당카페", "반려문화시설"};

    private final PlaceRepository placeRepository;
    private final PlaceVerificationService placeVerificationService;
    private final KakaoImageScraperService kakaoImageScraperService;
    private final RestClient restClient;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${pet-tour.service-key}")
    private String serviceKey;

    // @Transactional 삭제: 전체 루프를 하나로 묶지 않습니다.
    // @Async: 백그라운드 처리
    @Async
    @CacheEvict(value = "places", allEntries = true)
    public void runCultureBatch() {
        log.info("===== 문화시설 배치 시작 (서울/경기 × 3카테고리 필터 — 약 1,086건 예상) =====");
        int totalSaved = 0, totalSkipped = 0, totalDuplicated = 0;

        for (String region : REGIONS) {
            for (String category : CATEGORIES) {
                log.info("[KCISA] {} / {} 수집 중...", region, category);
                int page = 1;
                while (true) {
                    List<Map<String, Object>> items = fetchPage(page, region, category);
                    if (items == null || items.isEmpty()) break;

                    for (Map<String, Object> item : items) {
                        try {
                            int result = processItem(item);
                            if (result == 1)       totalSaved++;
                            else if (result == -1) totalDuplicated++;
                            else if (result == 0)  totalSkipped++;
                            // result == 2: 조기 필터(안전망) — Kakao 미호출이므로 sleep 생략
                            if (result != 2) {
                                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt(); break;
                                }
                            }
                        } catch (Exception e) {
                            log.error("[오류] {} — {}", item.get("시설명"), e.getMessage());
                            totalSkipped++;
                        }
                    }
                    if (items.size() < PAGE_SIZE) break;
                    page++;
                }
            }
        }
        log.info("===== 문화시설 배치 종료: 저장 {}건 / 중복 {}건 / 제외 {}건 =====",
                totalSaved, totalDuplicated, totalSkipped);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchPage(int page, String region, String category) {
        try {
            String encodedRegion   = UriUtils.encodeQueryParam(region, StandardCharsets.UTF_8);
            String encodedCategory = UriUtils.encodeQueryParam(category, StandardCharsets.UTF_8);
            String uriStr = API_URL
                    + "?page=" + page
                    + "&perPage=" + PAGE_SIZE
                    + "&returnType=JSON"
                    + "&serviceKey=" + serviceKey
                    + "&" + FILTER_REGION_KEY   + "=" + encodedRegion
                    + "&" + FILTER_CATEGORY_KEY + "=" + encodedCategory;

            Map<String, Object> response = restClient.get().uri(URI.create(uriStr)).retrieve().body(Map.class);
            if (response == null) return List.of();
            return (List<Map<String, Object>>) response.get("data");
        } catch (Exception e) {
            log.error("[KCISA API 오류] region={} category={} page={}: {}", region, category, page, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("null")
    @Transactional // 단건마다 트랜잭션 보장
    public int processItem(Map<String, Object> item) {
        String cat2 = str(item, "카테고리2");
        String cat3 = str(item, "카테고리3");
        String category = mapCategory(cat2, cat3);
        if (category == null) return 2; // Kakao 미호출 — 조기 필터

        String name = str(item, "시설명");
        String address = str(item, "도로명주소");
        String latStr = str(item, "위도");
        String lngStr = str(item, "경도");

        if (name == null || address == null || latStr == null || lngStr == null) return 2;
        if (!address.startsWith("서울") && !address.startsWith("경기")) return 2;

        double lat, lng;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            return 2; // Kakao 미호출 — 조기 필터
        }
        if (lat == 0.0 || lng == 0.0) return 2;

        // contentId 먼저 계산 — 기존 레코드 빠른 경로 분기용
        int hash = (name + "|" + address).hashCode();
        String contentId = "KCISA" + String.format("%08x", hash & 0xFFFFFFFFL);

        String phone    = str(item, "전화번호");
        String homepage = cleanHomepage(str(item, "홈페이지"));

        // KCISA 반려동물 전용 필드 (실제 API 필드명 사용)
        String petAvailable  = str(item, "반려동물 동반 가능정보"); // "Y" 등
        String animalSize    = str(item, "입장 가능 동물 크기");    // "소형", "모두 가능" 등
        String indoorYn      = str(item, "장소(실내) 여부");        // "Y"/"N"
        String outdoorYn     = str(item, "장소(실외)여부");         // "Y"/"N"
        String extraFee      = str(item, "애견 동반 추가 요금");    // "없음", "있음" 등
        String restrictions  = str(item, "반려동물 제한사항");      // 목줄필수, 이동장필요 등
        String petExtraInfo  = str(item, "반려동물 전용 정보");     // 추가 pet 전용 정보
        String operatingTime = str(item, "운영시간");               // 영업시간 실데이터
        String closedDays    = str(item, "휴무일");                 // 휴무일 실데이터

        // chkPetInside: 반려동물 동반 가능 여부 Y/N
        String chkPet = "Y".equalsIgnoreCase(petAvailable) ? "Y"
                      : (petAvailable != null && !petAvailable.isBlank()) ? "Y" : null;

        // 실내/실외 태그 조합
        String indoorOutdoorTag = null;
        if ("Y".equalsIgnoreCase(indoorYn) && "Y".equalsIgnoreCase(outdoorYn)) indoorOutdoorTag = "실내외";
        else if ("Y".equalsIgnoreCase(indoorYn))  indoorOutdoorTag = "실내";
        else if ("Y".equalsIgnoreCase(outdoorYn)) indoorOutdoorTag = "실외";

        // petTurnAdroose: 반려동물 제한사항 실데이터
        StringBuilder policyBuilder = new StringBuilder();
        if (extraFee != null && !extraFee.isBlank() && !"해당없음".equals(extraFee))
            policyBuilder.append("추가요금: ").append(extraFee).append("\n");
        if (restrictions != null && !restrictions.isBlank() && !"제한사항 없음".equals(restrictions))
            policyBuilder.append(restrictions).append("\n");
        if (petExtraInfo != null && !petExtraInfo.isBlank() && !"해당없음".equals(petExtraInfo))
            policyBuilder.append(petExtraInfo);
        String petDetail = policyBuilder.length() > 0 ? policyBuilder.toString().trim() : null;

        // operatingHours: 운영시간 + 휴무일 실데이터
        StringBuilder hoursBuilder = new StringBuilder();
        if (operatingTime != null && !operatingTime.isBlank()) hoursBuilder.append(operatingTime);
        if (closedDays != null && !closedDays.isBlank())
            hoursBuilder.append(hoursBuilder.length() > 0 ? " / 휴무: " : "휴무: ").append(closedDays);
        String finalHours = hoursBuilder.length() > 0 ? hoursBuilder.toString() : null;

        // ── 빠른 경로: 기존 레코드 → Kakao 미호출, pet 필드만 업데이트 ──────────
        Optional<Place> existingOpt = placeRepository.findByContentId(contentId);
        if (existingOpt.isPresent()) {
            Place existing = existingOpt.get();
            existing.upsertFromBatch(
                name, address, null,
                existing.getLatitude(), existing.getLongitude(), existing.getGeom(),
                category, null, phone,
                null, homepage,
                chkPet, null, petDetail, finalHours,
                true, existing.getKakaoId()
            );
            return 2; // Kakao 미호출 → sleep 생략
        }

        // ── 느린 경로: 신규 레코드 → 카카오 교차검증 + 저장 ─────────────────────
        PlaceVerificationService.VerificationResult verify =
                placeVerificationService.verify(name, address, lat, lng);
        if (!verify.confirmed()) return 0;

        if (verify.kakaoId() != null
                && placeRepository.findByKakaoId(verify.kakaoId()).isPresent()) {
            log.debug("[중복생략] '{}'", name);
            return -1;
        }

        Point geom = geometryFactory.createPoint(new Coordinate(verify.lng(), verify.lat()));
        String imageUrl = kakaoImageScraperService.scrapeImage(name, address);

        // tags: 카테고리 + 동물크기 + 실내외
        StringBuilder tagsBuilder = new StringBuilder(cat3 != null ? cat3 : (cat2 != null ? cat2 : ""));
        if (animalSize != null && !animalSize.isBlank() && !"해당없음".equals(animalSize))
            tagsBuilder.append(",").append(animalSize);
        if (indoorOutdoorTag != null)
            tagsBuilder.append(",").append(indoorOutdoorTag);
        String finalTags = tagsBuilder.toString().replaceAll("^,+", "");

        Place newPlace = Place.builder()
                .contentId(contentId)
                .kakaoId(verify.kakaoId())
                .title(name)
                .address(address)
                .latitude(verify.lat())
                .longitude(verify.lng())
                .geom(geom)
                .category(category)
                .imageUrl(imageUrl)
                .phone(phone)
                .homepage(homepage)
                .chkPetInside(chkPet)
                .petTurnAdroose(petDetail)
                .operatingHours(finalHours)
                .tags(finalTags)
                .isVerified(true)
                .build();
        placeRepository.save(newPlace);
        return 1;
    }

    private String mapCategory(String cat2, String cat3) {
        if (cat2 == null) return null;
        return switch (cat2) {
            case "반려동반여행" -> "펜션".equals(cat3) ? "STAY" : "PLACE";
            case "반려동물식당카페" -> "DINING";
            case "반려문화시설" -> "PLACE";
            default -> null;
        };
    }

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
