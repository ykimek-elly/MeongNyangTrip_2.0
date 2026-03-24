package com.team.meongnyang.place.dto;

import com.team.meongnyang.place.entity.Place;

/**
 * 관리자 검토 큐 — 보류 장소 응답 DTO.
 * AdminDashboard 장소검토 탭에서 사용.
 */
public record PendingPlaceDto(
        Long id,
        String title,
        String address,
        String addr2,
        Double latitude,
        Double longitude,
        String category,
        String imageUrl,
        String phone,
        String homepage,
        String pendingReason,
        String kakaoMapUrl
) {
    public static PendingPlaceDto from(Place p) {
        String kakaoMapUrl = String.format(
                "https://map.kakao.com/link/map/%s,%.6f,%.6f",
                p.getTitle(), p.getLatitude(), p.getLongitude());
        return new PendingPlaceDto(
                p.getId(),
                p.getTitle(),
                p.getAddress(),
                p.getAddr2(),
                p.getLatitude(),
                p.getLongitude(),
                p.getCategory(),
                p.getImageUrl(),
                p.getPhone(),
                p.getHomepage(),
                p.getPendingReason(),
                kakaoMapUrl
        );
    }
}
