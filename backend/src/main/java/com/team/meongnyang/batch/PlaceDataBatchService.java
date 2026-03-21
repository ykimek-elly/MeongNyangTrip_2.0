package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.PlaceVerificationService;
import com.team.meongnyang.place.service.KakaoImageScraperService;
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
import org.springframework.scheduling.annotation.Async;
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
 * [파이프라인 3.0]
 *   1단계: 한국관광공사 API → 서울(areaCode=1) + 경기(areaCode=31) 전체 수집
 *   2단계: 네이버+카카오 이중 교차검증 (폐업 확인)
 *          공식 이미지(firstimage) 수집 + 없는 경우 카카오맵 og:image 크롤링
 *   3단계: kakaoId 기준 중복 방지
 *   4단계: 확정 좌표·이미지로 DB Upsert
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
    private final KakaoImageScraperService kakaoImageScraperService;
    private final RestClient restClient;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Value("${pet-tour.service-key}")
    private String serviceKey;

    // @Transactional 삭제: 1,000건 이상의 전체 배치를 하나의 트랜잭션으로 잡으면 DB 연결 시간이 너무 길어져 타임아웃이 발생할 수 있습니다.
    // @Async: 백그라운드에서 실행하여 HTTP 타임아웃을 방지합니다.
    @Async
    @CacheEvict(value = "places", allEntries = true)
    public void runDailyBatch() {
        log.info("===== 장소 데이터 수집 배치 시작 (KTO + 카카오 스크래핑) =====");
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
                // Kakao API 보호를 위해 지연 시간을 1000ms(1초)로 상향 조정 (429 에러 방지용)
                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }
            log.info("[{}] 완료: 저장 {}건 / 중복 {}건 / 제외 {}건",
                    regionName, totalSaved, totalDuplicated, totalSkipped);
        }
        log.info("===== 배치 종료: 총 저장 {}건 / 중복 {}건 / 제외 {}건 =====",
                totalSaved, totalDuplicated, totalSkipped);
    }

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

            if (response == null || response.getResponse() == null || response.getResponse().getBody() == null
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

    @SuppressWarnings("null")
    @Transactional // 개별 아이템 처리마다 트랜잭션을 분리하여 안전하게 저장합니다.
    public int processItem(PetTourApiResponse.Item item) {
        if (item.getContentid() == null || item.getMapx() == null || item.getMapy() == null
                || item.getMapx().isBlank() || item.getMapy().isBlank()) {
            return 0;
        }

        double publicLng = Double.parseDouble(item.getMapx());
        double publicLat = Double.parseDouble(item.getMapy());

        String fullAddress = item.getAddr1();
        if (item.getAddr2() != null && !item.getAddr2().isBlank()) {
            fullAddress = item.getAddr1() + " " + item.getAddr2().trim();
        }
        final String finalAddress = fullAddress;

        // ─── 2단계: 네이버+카카오 교차검증 ─────────────────────────────
        PlaceVerificationService.VerificationResult verify =
                placeVerificationService.verify(item.getTitle(), finalAddress, publicLat, publicLng);

        if (!verify.confirmed()) return 0; // 폐업/미존재

        if (verify.kakaoId() != null
                && placeRepository.findByKakaoId(verify.kakaoId()).isPresent()) {
            log.debug("[중복생략] '{}'", item.getTitle());
            return -1;
        }

        Point geom = geometryFactory.createPoint(new Coordinate(verify.lng(), verify.lat()));

        DetailPetTourResponse.Item petDetail = fetchDetailPetTour(item.getContentid());
        String chkPetInside   = petDetail != null ? petDetail.getChkpetinside()   : null;
        String accomCountPet  = petDetail != null ? petDetail.getAccomcountpet()  : null;
        String petTurnAdroose = petDetail != null ? petDetail.getPetturnadroose() : null;

        CommonDetail commonDetail = fetchCommonDetail(item.getContentid());
        String overview = commonDetail.overview();
        String homepage = commonDetail.homepage();

        // [핵심] 이미지: 공공데이터 원본 우선, 없으면 Kakao Image Search API로 검색
        String imageUrl = (item.getFirstimage() != null && !item.getFirstimage().isBlank())
                ? item.getFirstimage()
                : kakaoImageScraperService.scrapeImage(item.getTitle(), finalAddress);

        String category = determineCategory(item.getContenttypeid());

        placeRepository.findByContentId(item.getContentid()).ifPresentOrElse(
            existing -> existing.upsertFromBatch(
                item.getTitle(), finalAddress, item.getAddr2(),
                verify.lat(), verify.lng(), geom,
                category, imageUrl, item.getTel(), overview, homepage,
                chkPetInside, accomCountPet, petTurnAdroose, null, true, verify.kakaoId()
            ),
            () -> {
                Place newPlace = Place.builder()
                    .contentId(item.getContentid())
                    .kakaoId(verify.kakaoId())
                    .title(item.getTitle())
                    .address(finalAddress)
                    .addr2(item.getAddr2())
                    .latitude(verify.lat())
                    .longitude(verify.lng())
                    .geom(geom)
                    .category(category)
                    .imageUrl(imageUrl)
                    .phone(item.getTel())
                    .overview(overview)
                    .homepage(homepage)
                    .chkPetInside(chkPetInside)
                    .accomCountPet(accomCountPet)
                    .petTurnAdroose(petTurnAdroose)
                    .isVerified(true)
                    .build();
                placeRepository.save(newPlace);
            }
        );
        return 1;
    }

    private DetailPetTourResponse.Item fetchDetailPetTour(String contentId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailPetTour2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("contentId", contentId)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "MeongNyangTrip")
                    .queryParam("_type", "json").build(true).toUri();
            DetailPetTourResponse response = restClient.get().uri(uri).retrieve().body(DetailPetTourResponse.class);
            if (response == null || response.getResponse() == null || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null
                    || response.getResponse().getBody().getItems().getItem().isEmpty()) {
                return null;
            }
            return response.getResponse().getBody().getItems().getItem().get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private record CommonDetail(String overview, String homepage) {}

    private CommonDetail fetchCommonDetail(String contentId) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailCommon2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("contentId", contentId)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "MeongNyangTrip")
                    .queryParam("_type", "json").build(true).toUri();
            DetailCommonResponse response = restClient.get().uri(uri).retrieve().body(DetailCommonResponse.class);
            if (response == null || response.getResponse() == null || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null
                    || response.getResponse().getBody().getItems().getItem().isEmpty()) {
                return new CommonDetail(null, null);
            }
            DetailCommonResponse.Item it = response.getResponse().getBody().getItems().getItem().get(0);
            return new CommonDetail(it.getOverview(), it.getHomepage());
        } catch (Exception e) {
            return new CommonDetail(null, null);
        }
    }

    private String determineCategory(String contentTypeId) {
        if ("32".equals(contentTypeId)) return "STAY";
        if ("39".equals(contentTypeId)) return "DINING";
        return "PLACE";
    }
}
