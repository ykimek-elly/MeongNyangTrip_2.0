package com.team.meongnyang.place.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.place.dto.PlaceRequestDto;
import com.team.meongnyang.place.dto.PlaceResponseDto;
import com.team.meongnyang.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 장소(Place) REST API Controller.
 * 모든 응답은 ApiResponse<T> 규격을 따른다.
 *
 * @see docs/specs/core-setup.md 1장 참조
 */
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    /**
     * 장소 목록 조회
     *
     * [위치 기반] GET /api/v1/places?lat=37.5&lng=126.9&radius=5000&category=STAY
     *   → PostGIS ST_DWithin 반경 검색 + Redis 캐싱
     *
     * [키워드 기반] GET /api/v1/places?category=STAY&keyword=서울숲
     *   → 위치 정보 없을 때 fallback
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponseDto>>> getPlaces(
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false, defaultValue = "5000") int radius,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String keyword
    ) {
        List<PlaceResponseDto> places;
        if (lat != null && lng != null) {
            places = placeService.getPlacesNearby(lat, lng, radius, category);
        } else {
            places = placeService.getPlaces(category, keyword);
        }
        return ResponseEntity.ok(ApiResponse.success("장소 목록 조회 성공", places));
    }

    /**
     * 장소 상세 조회
     * GET /api/v1/places/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceResponseDto>> getPlace(@PathVariable Long id) {
        PlaceResponseDto place = placeService.getPlace(id);
        return ResponseEntity.ok(
            ApiResponse.success("장소 상세 조회 성공", place)
        );
    }

    /**
     * 장소 등록
     * POST /api/v1/places
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PlaceResponseDto>> createPlace(
        @Valid @RequestBody PlaceRequestDto request
    ) {
        PlaceResponseDto created = placeService.createPlace(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("장소 등록 성공", created));
    }

    /**
     * 장소 수정
     * PUT /api/v1/places/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceResponseDto>> updatePlace(
        @PathVariable Long id,
        @Valid @RequestBody PlaceRequestDto request
    ) {
        PlaceResponseDto updated = placeService.updatePlace(id, request);
        return ResponseEntity.ok(
            ApiResponse.success("장소 수정 성공", updated)
        );
    }

    /**
     * 장소 삭제
     * DELETE /api/v1/places/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.ok(
            ApiResponse.success("장소 삭제 성공", null)
        );
    }
}
