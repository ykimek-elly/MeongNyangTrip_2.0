package com.team.meongnyang.place.service;

import com.team.meongnyang.place.dto.PendingPlaceDto;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.entity.PlaceStatus;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.review.repository.ReviewRepository;
import com.team.meongnyang.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 장소 검토 큐 서비스.
 * Stage2 유사도 50~79% 보류 장소를 관리자가 수동 검토/승인/거절한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceAdminService {

    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final NaverLocalVerifyService naverLocalVerifyService;
    private final KakaoLocalVerifyService kakaoLocalVerifyService;
    private static final GeometryFactory GEO_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    /** 보류 목록 조회 */
    @Transactional(readOnly = true)
    public List<PendingPlaceDto> getPendingPlaces() {
        return placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.PENDING)
                .stream()
                .map(PendingPlaceDto::from)
                .toList();
    }

    /** 거절 목록 조회 — 관리자 수동 복구용 */
    @Transactional(readOnly = true)
    public List<PendingPlaceDto> getRejectedPlaces() {
        return placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.REJECTED)
                .stream()
                .map(PendingPlaceDto::from)
                .toList();
    }

    /** 이미지 없는 ACTIVE 장소 목록 조회 — 관리자 수동 이미지 등록용 */
    @Transactional(readOnly = true)
    public List<PendingPlaceDto> getNoImagePlaces() {
        return placeRepository.findByImageUrlIsNullOrEmpty()
                .stream()
                .filter(p -> PlaceStatus.ACTIVE.equals(p.getStatus()))
                .map(PendingPlaceDto::from)
                .toList();
    }

    /** 전체 ACTIVE 장소 조회 — 관리자 수정용 (이미지 없는 장소 포함) */
    @Transactional(readOnly = true)
    public List<PendingPlaceDto> getAllActivePlaces() {
        return placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.ACTIVE)
                .stream()
                .map(PendingPlaceDto::from)
                .toList();
    }

    /** 장소 필드 수정 — null은 기존값 유지, 캐시 무효화 */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public PendingPlaceDto editPlace(Long id, String title, String address,
                                     String phone, String homepage, String imageUrl) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + id));
        place.patchAdminFields(title, address, phone, homepage, imageUrl);
        return PendingPlaceDto.from(place);
    }

    /** 이미지 URL 수동 등록 — 등록 즉시 공개 캐시 무효화 */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void updateImage(Long id, String imageUrl) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + id));
        place.enrichImageFromNaver(imageUrl);
    }

    /**
     * 승인 — 좌표 수정 후 ACTIVE 전환.
     * lat/lng가 null이면 기존 좌표 유지.
     */
    @Transactional
    public PendingPlaceDto approve(Long id, Double lat, Double lng) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + id));

        org.locationtech.jts.geom.Point geom = null;
        if (lat != null && lng != null) {
            geom = GEO_FACTORY.createPoint(new Coordinate(lng, lat));
            geom.setSRID(4326);
        }
        place.approveFromPending(lat, lng, geom);
        return PendingPlaceDto.from(place);
    }

    /** 거절 — REJECTED 전환 */
    @Transactional
    public void reject(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + id));
        place.rejectFromPending();
    }

    /**
     * 수동 수정 후 승인.
     * 상호명, 주소, 좌표를 관리자가 직접 입력하고 ACTIVE 전환.
     */
    @Transactional
    public PendingPlaceDto manualApprove(Long id, String title, String address,
                                         Double lat, Double lng) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + id));

        org.locationtech.jts.geom.Point geom = null;
        if (lat != null && lng != null) {
            geom = GEO_FACTORY.createPoint(new Coordinate(lng, lat));
            geom.setSRID(4326);
        }
        // 필드 수정은 기존 update() 메서드 활용
        place.update(
                title != null ? title : place.getTitle(),
                place.getDescription(),
                address != null ? address : place.getAddress(),
                lat != null ? lat : place.getLatitude(),
                lng != null ? lng : place.getLongitude(),
                place.getCategory(),
                place.getImageUrl(),
                place.getPhone(),
                place.getTags()
        );
        place.approveFromPending(lat, lng, geom);
        return PendingPlaceDto.from(place);
    }

    /**
     * AI 보강 미리보기 — DB 저장 없이 Naver 블로그 분석 + aiRating 계산만 수행.
     * FE에서 "AI 보강 분석" 버튼 클릭 시 호출.
     * 반환: { aiRating, blogCount, blogPositiveTags, blogNegativeTags, naverVerified }
     */
    public Map<String, Object> analyzePlacePreview(String title, String address,
                                                    String phone, String homepage,
                                                    String imageUrl, String description) {
        // 1. Kakao 주소 → 좌표 변환
        double[] coords = kakaoLocalVerifyService.geocodeAddress(address);
        Double lat = coords != null ? coords[0] : null;
        Double lng = coords != null ? coords[1] : null;

        // 2. 임시 Place 객체 (DB 저장 없음)
        Place temp = Place.builder()
                .title(title)
                .address(address)
                .phone(phone)
                .homepage(homepage)
                .imageUrl(imageUrl)
                .overview(description)
                .isVerified(true)
                .build();

        // 3. Naver 운영 교차검증
        NaverLocalVerifyService.VerifyResult verify =
                naverLocalVerifyService.verify(title, address);
        boolean naverVerified = verify.isActive();

        // 4. 블로그 데이터 수집 + AI 별점 계산
        NaverLocalVerifyService.BlogResult blog = naverVerified
                ? naverLocalVerifyService.fetchBlogData(title)
                : NaverLocalVerifyService.BlogResult.empty();
        temp.computeAiRating(blog.total(), blog.latestPostDate(), blog.descriptions());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lat",              lat);
        result.put("lng",              lng);
        result.put("geocodeSuccess",   coords != null);
        result.put("aiRating",         temp.getAiRating());
        result.put("blogCount",        blog.total());
        result.put("blogPositiveTags", temp.getBlogPositiveTags());
        result.put("blogNegativeTags", temp.getBlogNegativeTags());
        result.put("naverVerified",    naverVerified);
        return result;
    }

    /**
     * 신규 장소 수동 등록 — 관리자가 직접 ACTIVE 상태로 생성.
     * contentId는 "ADMIN-{timestamp}" 형식으로 자동 부여.
     * aiRating은 analyzePlacePreview() 결과를 전달하거나 null이면 자동 계산.
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public PendingPlaceDto createPlace(String title, String category, String address,
                                       Double lat, Double lng,
                                       String phone, String homepage,
                                       String imageUrl, String description,
                                       Double aiRating) {
        org.locationtech.jts.geom.Point geom = GEO_FACTORY.createPoint(
                new Coordinate(lng, lat));
        geom.setSRID(4326);

        Place place = Place.builder()
                .contentId("ADMIN-" + System.currentTimeMillis())
                .title(title)
                .category(category)
                .address(address)
                .latitude(lat)
                .longitude(lng)
                .geom(geom)
                .phone(phone)
                .homepage(homepage)
                .imageUrl(imageUrl)
                .description(description)
                .overview(description)
                .status(PlaceStatus.ACTIVE)
                .isVerified(true)
                .aiRating(aiRating)
                .build();
        placeRepository.save(place);
        return PendingPlaceDto.from(place);
    }

    /** 장소 영구 삭제 (중복 제거용) — 연관 리뷰·찜 먼저 삭제 후 본체 삭제 */
    @Transactional
    @CacheEvict(value = "places", allEntries = true)
    public void deletePlace(Long id) {
        reviewRepository.deleteByPlace_Id(id);
        wishlistRepository.deleteByPlace_Id(id);
        placeRepository.findById(id).ifPresent(placeRepository::delete);
    }
}
