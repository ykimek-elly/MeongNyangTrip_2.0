package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.KakaoLocalVerifyService;
import com.team.meongnyang.pettour.dto.PetTourApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 공공데이터 장소 수집 배치 서비스.
 *
 * 매일 새벽 2시 자동 실행:
 * 1. 한국관광공사 API → 서울(areaCode=1) + 경기(areaCode=31) 전체 수집
 * 2. Kakao Local API 교차검증 (1단계: 좌표 정합, 2단계: 영업 확인)
 * 3. content_id 기준 DB Upsert
 * 4. Redis 캐시 무효화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceDataBatchService {

    private static final int[] AREA_CODES = {1, 31}; // 서울=1, 경기=31
    private static final int PAGE_SIZE = 100;
    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService2";

    private final PlaceRepository placeRepository;
    private final KakaoLocalVerifyService kakaoVerifyService;
    private final RestClient restClient;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${pet-tour.service-key}")
    private String serviceKey;

    /**
     * 매일 새벽 2시 자동 실행.
     * 수동 실행 시: POST /api/v1/admin/batch/places (PlaceBatchController 통해 호출 가능)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    @CacheEvict(value = "places", allEntries = true)
    public void runDailyBatch() {
        log.info("===== 장소 데이터 배치 시작 =====");
        int totalSaved = 0;
        int totalSkipped = 0;

        for (int areaCode : AREA_CODES) {
            String regionName = areaCode == 1 ? "서울" : "경기";
            log.info("[{}] 수집 시작", regionName);

            List<PetTourApiResponse.Item> items = fetchAllPages(areaCode);
            log.info("[{}] 공공데이터 수집 완료: {}건", regionName, items.size());

            for (PetTourApiResponse.Item item : items) {
                try {
                    boolean saved = processItem(item);
                    if (saved) totalSaved++;
                    else totalSkipped++;
                } catch (Exception e) {
                    log.error("[처리오류] contentId={} — {}", item.getContentid(), e.getMessage());
                    totalSkipped++;
                }
            }
            log.info("[{}] 완료: 저장 {}건 / 제외 {}건", regionName, totalSaved, totalSkipped);
        }
        log.info("===== 배치 종료: 총 저장 {}건 / 제외 {}건 =====", totalSaved, totalSkipped);
    }

    /** 공공API 전체 페이지 수집 (areaCode 기반) */
    private List<PetTourApiResponse.Item> fetchAllPages(int areaCode) {
        List<PetTourApiResponse.Item> allItems = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/areaBasedList2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("numOfRows", PAGE_SIZE)
                    .queryParam("pageNo", pageNo)
                    .queryParam("areaCode", areaCode)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "MeongNyangTrip")
                    .queryParam("_type", "json")
                    .build(true)
                    .toUri();

            PetTourApiResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(PetTourApiResponse.class);

            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null) {
                break;
            }

            List<PetTourApiResponse.Item> pageItems = response.getResponse().getBody().getItems().getItem();
            allItems.addAll(pageItems);

            int totalCount = response.getResponse().getBody().getTotalCount();
            if (allItems.size() >= totalCount || pageItems.size() < PAGE_SIZE) break;

            pageNo++;
        }
        return allItems;
    }

    /**
     * 단건 처리: 교차검증 → Upsert
     * @return true=저장, false=건너뜀
     */
    @SuppressWarnings("null")
    private boolean processItem(PetTourApiResponse.Item item) {
        if (item.getContentid() == null || item.getMapx() == null || item.getMapy() == null
                || item.getMapx().isBlank() || item.getMapy().isBlank()) {
            return false;
        }

        double publicLng = Double.parseDouble(item.getMapx());
        double publicLat = Double.parseDouble(item.getMapy());

        // Kakao 교차검증 (1단계 좌표 정합 + 2단계 영업 확인)
        KakaoLocalVerifyService.VerifyResult result =
                kakaoVerifyService.verify(item.getTitle(), publicLat, publicLng);

        if (!result.isActive()) return false; // 폐업/미존재 → 저장 제외

        // PostGIS Point 생성 (경도, 위도 순서)
        Point geom = geometryFactory.createPoint(new Coordinate(result.lng(), result.lat()));

        // Upsert: contentId 기준
        String category = determineCategory(item.getContenttypeid());
        placeRepository.findByContentId(item.getContentid()).ifPresentOrElse(
            existing -> existing.upsertFromBatch(
                item.getTitle(),
                item.getAddr1(),
                result.lat(),
                result.lng(),
                geom,
                category,
                item.getFirstimage(),
                item.getTel(),
                true
            ),
            () -> {
                Place newPlace = Place.builder()
                    .contentId(item.getContentid())
                    .title(item.getTitle())
                    .address(item.getAddr1())
                    .latitude(result.lat())
                    .longitude(result.lng())
                    .geom(geom)
                    .category(category)
                    .imageUrl(item.getFirstimage())
                    .phone(item.getTel())
                    .isVerified(true)
                    .build();
                placeRepository.save(newPlace);
            }
        );
        return true;
    }

    private String determineCategory(String contentTypeId) {
        if ("32".equals(contentTypeId)) return "STAY";
        if ("39".equals(contentTypeId)) return "DINING";
        return "PLACE";
    }
}
