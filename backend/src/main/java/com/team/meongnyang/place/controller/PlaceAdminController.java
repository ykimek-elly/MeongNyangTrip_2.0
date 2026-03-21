package com.team.meongnyang.place.controller;

import com.team.meongnyang.place.dto.PendingPlaceDto;
import com.team.meongnyang.place.service.PlaceAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 장소 검토 큐 API.
 * AdminDashboard 장소검토 탭에서 호출.
 *
 * GET  /api/v1/admin/places/pending          — 보류 목록 조회
 * POST /api/v1/admin/places/{id}/approve     — 승인 (좌표 수정 옵션)
 * POST /api/v1/admin/places/{id}/reject      — 거절
 * PUT  /api/v1/admin/places/{id}/manual      — 수동 수정 후 승인
 */
@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
public class PlaceAdminController {

    private final PlaceAdminService placeAdminService;

    /** 전체 ACTIVE 장소 조회 — GET /api/v1/admin/places */
    @GetMapping
    public ResponseEntity<List<PendingPlaceDto>> getAllActivePlaces() {
        return ResponseEntity.ok(placeAdminService.getAllActivePlaces());
    }

    /** 장소 필드 수정 — PATCH /api/v1/admin/places/{id}/edit */
    @PatchMapping("/{id}/edit")
    public ResponseEntity<PendingPlaceDto> editPlace(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(placeAdminService.editPlace(
                id,
                body.get("title"),
                body.get("address"),
                body.get("phone"),
                body.get("homepage"),
                body.get("imageUrl")));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingPlaceDto>> getPendingPlaces() {
        return ResponseEntity.ok(placeAdminService.getPendingPlaces());
    }

    /** 거절 목록 조회 — GET /api/v1/admin/places/rejected */
    @GetMapping("/rejected")
    public ResponseEntity<List<PendingPlaceDto>> getRejectedPlaces() {
        return ResponseEntity.ok(placeAdminService.getRejectedPlaces());
    }

    /** 이미지 없는 장소 목록 조회 — GET /api/v1/admin/places/no-image */
    @GetMapping("/no-image")
    public ResponseEntity<List<PendingPlaceDto>> getNoImagePlaces() {
        return ResponseEntity.ok(placeAdminService.getNoImagePlaces());
    }

    /** 이미지 URL 수동 등록 — PATCH /api/v1/admin/places/{id}/image */
    @PatchMapping("/{id}/image")
    public ResponseEntity<Void> updateImage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        placeAdminService.updateImage(id, body.get("imageUrl"));
        return ResponseEntity.noContent().build();
    }

    /**
     * 승인 — 좌표 수정 포함 가능.
     * body: { "lat": 37.123, "lng": 126.456 }  (생략 시 기존 좌표 유지)
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<PendingPlaceDto> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Double> body) {
        Double lat = body != null ? body.get("lat") : null;
        Double lng = body != null ? body.get("lng") : null;
        return ResponseEntity.ok(placeAdminService.approve(id, lat, lng));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        placeAdminService.reject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 수동 수정 후 승인.
     * body: { "title": "...", "address": "...", "lat": 37.123, "lng": 126.456 }
     */
    @PutMapping("/{id}/manual")
    public ResponseEntity<PendingPlaceDto> manualApprove(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String title   = (String) body.get("title");
        String address = (String) body.get("address");
        Double lat = body.get("lat") != null ? ((Number) body.get("lat")).doubleValue() : null;
        Double lng = body.get("lng") != null ? ((Number) body.get("lng")).doubleValue() : null;
        return ResponseEntity.ok(placeAdminService.manualApprove(id, title, address, lat, lng));
    }
}
