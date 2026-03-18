package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.PlaceVerificationService;
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
 * [파이프라인 2.0 — 검증 후 저장]
 *   1단계: 한국관광공사 API → 서울(areaCode=1) + 경기(areaCode=31) 전체 수집
 *   2단계: PlaceVerificationService(네이버+카카오) 이중 교차검증
 *          - 이름 유사도 + 좌표 500m 일치 + 지역 컨텍스트
 *          - 둘 다 실패 시 저장 제외
 *   3단계: kakaoId 기준 데이터셋 간 중복 방지 (KCISA 교차)
 *   4단계: 확정 좌표·이미지로 DB Upsert (isVerified=true)
 *   5단계: Redis 캐시 무효화
 *
 * 수동 실행: POST /api/v1/admin/batch/places
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceDataBatchService {

    private static final int[] AREA_CODES = {1, 31}; // 서울=1, 경기=31
    private static final int PAGE_SIZE = 100;
    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService2";

    private final PlaceRepository placeRepository;
    private final PlaceVerificationService placeVerificationService;
    private final RestClient restClient;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${pet-tour.service-key}")
    private String serviceKey;

    /**
     * 수동 실행 전용.
     * POST /api/v1/admin/batch/places
     */
    @Transactional
    @CacheEvict(value = "places", allEntries = true)
    public void runDailyBatch() {
        log.info("===== 장소 데이터 배치 시작 (네이버+카카오 이중검증) =====");
        int totalSaved = 0;
        int totalSkipped = 0;
        int totalDuplicated = 0;

        for (int areaCode : AREA_CODES) {
            String regionName = areaCode == 1 ? "서울" : "경기";
            log.info("[{}] 수집 시작", regionName);

            List<PetTourApiResponse.Item> items = fetchAllPages(areaCode);
            log.info("[{}] 공공데이터 수집 완료: {}건", regionName, items.size());

            for (PetTourApiResponse.Item item : items) {
                try {
                    int result = processItem(item);
                    if (result == 1)       totalSaved++;
                    else if (result == -1) totalDuplicated++;
                    else                   totalSkipped++;
                } catch (Exception e) {
                    log.error("[처리오류] contentId={} — {}", item.getContentid(), e.getMessage());
                    totalSkipped++;
                }
                // Naver+Kakao API 속도 제한 (각 10 req/s) 준수
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }
            log.info("[{}] 완료: 저장 {}건 / 중복 {}건 / 제외 {}건",
                    regionName, totalSaved, totalDuplicated, totalSkipped);
        }
        log.info("===== 배치 종료: 총 저장 {}건 / 중복 {}건 / 제외 {}건 =====",
                totalSaved, totalDuplicated, totalSkipped);
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
     * 단건 처리: 교차검증 → 중복체크 → 상세정보 수집 → Upsert
     *
     * @return 1=저장, -1=중복생략, 0=건너뜀(검증실패/오류)
     */
    @SuppressWarnings("null")
    private int processItem(PetTourApiResponse.Item item) {
        if (item.getContentid() == null || item.getMapx() == null || item.getMapy() == null
                || item.getMapx().isBlank() || item.getMapy().isBlank()) {
            return 0;
        }

        double publicLng = Double.parseDouble(item.getMapx());
        double publicLat = Double.parseDouble(item.getMapy());

        // addr1 + addr2 합산 (지역 컨텍스트 추출용)
        String fullAddress = item.getAddr1();
        if (item.getAddr2() != null && !item.getAddr2().isBlank()) {
            fullAddress = item.getAddr1() + " " + item.getAddr2().trim();
        }
        final String finalAddress = fullAddress;

        // ─── 2단계: 네이버+카카오 이중 교차검증 ─────────────────────────────
        PlaceVerificationService.VerificationResult verify =
                placeVerificationService.verify(item.getTitle(), finalAddress, publicLat, publicLng);

        if (!verify.confirmed()) return 0; // 폐업/미존재 → 저장 제외

        // ─── 중복 체크: kakaoId 기준 (KCISA 교차 dedup) ──────────────────────
        if (verify.kakaoId() != null
                && placeRepository.findByKakaoId(verify.kakaoId()).isPresent()) {
            log.debug("[공공데이터 중복-생략] '{}' — kakaoId={}", item.getTitle(), verify.kakaoId());
            return -1;
        }

        // ─── 3단계: 확정 좌표 + 상세정보 수집 ───────────────────────────────
        Point geom = geometryFactory.createPoint(
                new Coordinate(verify.lng(), verify.lat()));

        DetailPetTourResponse.Item petDetail = fetchDetailPetTour(item.getContentid());
        String chkPetInside   = petDetail != null ? petDetail.getChkpetinside()   : null;
        String accomCountPet  = petDetail != null ? petDetail.getAccomcountpet()  : null;
        String petTurnAdroose = petDetail != null ? petDetail.getPetturnadroose() : null;

        CommonDetail commonDetail = fetchCommonDetail(item.getContentid());
        String overview = commonDetail.overview();
        String homepage = commonDetail.homepage();

        // 이미지: 공공데이터 원본 우선, 없으면 Naver에서 취득한 이미지 사용
        String imageUrl = (item.getFirstimage() != null && !item.getFirstimage().isBlank())
                ? item.getFirstimage()
                : verify.imageUrl();

        String category     = determineCategory(item.getContenttypeid());
        String finalAddr2   = item.getAddr2();
        String fKakaoId     = verify.kakaoId();
        String fChkPet      = chkPetInside;
        String fAccomCount  = accomCountPet;
        String fPetAdroose  = petTurnAdroose;
        String fOverview    = overview;
        String fHomepage    = homepage;
        String fImageUrl    = imageUrl;

        // ─── 4단계: contentId 기준 Upsert ────────────────────────────────────
        placeRepository.findByContentId(item.getContentid()).ifPresentOrElse(
            existing -> existing.upsertFromBatch(
                item.getTitle(),
                finalAddress,
                finalAddr2,
                verify.lat(),
                verify.lng(),
                geom,
                category,
                fImageUrl,
                item.getTel(),
                fOverview,
                fHomepage,
                fChkPet,
                fAccomCount,
                fPetAdroose,
                true,
                fKakaoId
            ),
            () -> {
                Place newPlace = Place.builder()
                    .contentId(item.getContentid())
                    .kakaoId(fKakaoId)
                    .title(item.getTitle())
                    .address(finalAddress)
                    .addr2(finalAddr2)
                    .latitude(verify.lat())
                    .longitude(verify.lng())
                    .geom(geom)
                    .category(category)
                    .imageUrl(fImageUrl)
                    .phone(item.getTel())
                    .overview(fOverview)
                    .homepage(fHomepage)
                    .chkPetInside(fChkPet)
                    .accomCountPet(fAccomCount)
                    .petTurnAdroose(fPetAdroose)
                    .isVerified(true)
                    .build();
                placeRepository.save(newPlace);
            }
        );
        return 1;
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
            DetailCommonResponse.Item it = response.getResponse().getBody().getItems().getItem().get(0);
            return new CommonDetail(it.getOverview(), it.getHomepage());
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
