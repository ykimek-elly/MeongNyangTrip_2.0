package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.KakaoLocalVerifyService;
import com.team.meongnyang.pettour.dto.DetailCommonResponse;
import com.team.meongnyang.pettour.dto.DetailPetTourResponse;
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
                // Kakao API 속도 제한(10 req/s) 준수
                try { Thread.sleep(120); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
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
     * 단건 처리: 교차검증 → 상세정보 수집 → Upsert
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

        // 반려동물 상세정보 수집 (detailPetTour2)
        DetailPetTourResponse.Item petDetail = fetchDetailPetTour(item.getContentid());
        String chkPetInside   = petDetail != null ? petDetail.getChkpetinside()   : null;
        String accomCountPet  = petDetail != null ? petDetail.getAccomcountpet()  : null;
        String petTurnAdroose = petDetail != null ? petDetail.getPetturnadroose() : null;

        // 소개글 + 홈페이지 수집 (detailCommon2)
        CommonDetail commonDetail = fetchCommonDetail(item.getContentid());
        String overview = commonDetail.overview();
        String homepage = commonDetail.homepage();

        // addr1 + addr2 합산
        String fullAddress = item.getAddr1();
        if (item.getAddr2() != null && !item.getAddr2().isBlank()) {
            fullAddress = item.getAddr1() + " " + item.getAddr2().trim();
        }
        final String finalAddress = fullAddress;
        final String finalAddr2 = item.getAddr2();

        // Upsert: contentId 기준
        String category = determineCategory(item.getContenttypeid());
        final String finalChkPetInside = chkPetInside;
        final String finalAccomCountPet = accomCountPet;
        final String finalPetTurnAdroose = petTurnAdroose;
        final String finalOverview = overview;
        final String finalHomepage = homepage;

        placeRepository.findByContentId(item.getContentid()).ifPresentOrElse(
            existing -> existing.upsertFromBatch(
                item.getTitle(),
                finalAddress,
                finalAddr2,
                result.lat(),
                result.lng(),
                geom,
                category,
                item.getFirstimage(),
                item.getTel(),
                finalOverview,
                finalHomepage,
                finalChkPetInside,
                finalAccomCountPet,
                finalPetTurnAdroose,
                true
            ),
            () -> {
                Place newPlace = Place.builder()
                    .contentId(item.getContentid())
                    .title(item.getTitle())
                    .address(finalAddress)
                    .addr2(finalAddr2)
                    .latitude(result.lat())
                    .longitude(result.lng())
                    .geom(geom)
                    .category(category)
                    .imageUrl(item.getFirstimage())
                    .phone(item.getTel())
                    .overview(finalOverview)
                    .homepage(finalHomepage)
                    .chkPetInside(finalChkPetInside)
                    .accomCountPet(finalAccomCountPet)
                    .petTurnAdroose(finalPetTurnAdroose)
                    .isVerified(true)
                    .build();
                placeRepository.save(newPlace);
            }
        );
        return true;
    }

    /** detailPetTour2 API 호출 — 반려동물 동반 상세정보 */
    private DetailPetTourResponse.Item fetchDetailPetTour(String contentId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailPetTour2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("contentId", contentId)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "MeongNyangTrip")
                    .queryParam("_type", "json")
                    .build(true)
                    .toUri();

            DetailPetTourResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DetailPetTourResponse.class);

            if (response == null || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null
                    || response.getResponse().getBody().getItems().getItem().isEmpty()) {
                return null;
            }
            return response.getResponse().getBody().getItems().getItem().get(0);
        } catch (Exception e) {
            log.warn("[detailPetTour2 오류] contentId={} — {}", contentId, e.getMessage());
            return null;
        }
    }

    /** detailCommon2 API 호출 — overview(소개글) + homepage(홈페이지) 수집 */
    private record CommonDetail(String overview, String homepage) {}

    private CommonDetail fetchCommonDetail(String contentId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailCommon2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("contentId", contentId)
                    .queryParam("overviewYN", "Y")
                    .queryParam("homepageYN", "Y")
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "MeongNyangTrip")
                    .queryParam("_type", "json")
                    .build(true)
                    .toUri();

            DetailCommonResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(DetailCommonResponse.class);

            if (response == null || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null
                    || response.getResponse().getBody().getItems().getItem().isEmpty()) {
                return new CommonDetail(null, null);
            }
            DetailCommonResponse.Item item = response.getResponse().getBody().getItems().getItem().get(0);
            return new CommonDetail(item.getOverview(), item.getHomepage());
        } catch (Exception e) {
            log.warn("[detailCommon2 오류] contentId={} — {}", contentId, e.getMessage());
            return new CommonDetail(null, null);
        }
    }

    private String determineCategory(String contentTypeId) {
        if ("32".equals(contentTypeId)) return "STAY";
        if ("39".equals(contentTypeId)) return "DINING";
        return "PLACE";
    }
}
