package com.team.meongnyang.place.service;

import com.team.meongnyang.place.dto.PendingPlaceDto;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.entity.PlaceStatus;
import com.team.meongnyang.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 장소 검토 큐 서비스.
 * Stage2 유사도 50~79% 보류 장소를 관리자가 수동 검토/승인/거절한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceAdminService {

    private final PlaceRepository placeRepository;
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
}
