package com.team.meongnyang.place.dto;

import com.team.meongnyang.place.entity.Place;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 장소 응답 DTO.
 * Entity의 내부 구조를 직접 노출하지 않고, 필요한 데이터만 가공하여 반환한다.
 */
@Getter
@Builder
public class PlaceResponseDto {

    private Long id;
    private String title;
    private String description;
    private String address;
    private String addr2;
    private Double latitude;
    private Double longitude;
    private String category;
    private Double rating;
    private Integer reviewCount;
    private String imageUrl;
    private String phone;
    private String tags;
    private String overview;
    private String chkPetInside;
    private String accomCountPet;
    private String petTurnAdroose;
    private String homepage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Entity → ResponseDto 변환 팩토리 메서드 */
    public static PlaceResponseDto from(Place place) {
        return PlaceResponseDto.builder()
            .id(place.getId())
            .title(place.getTitle())
            .description(place.getDescription())
            .address(place.getAddress())
            .addr2(place.getAddr2())
            .latitude(place.getLatitude())
            .longitude(place.getLongitude())
            .category(place.getCategory())
            .rating(place.getRating())
            .reviewCount(place.getReviewCount())
            .imageUrl(place.getImageUrl())
            .phone(place.getPhone())
            .tags(place.getTags())
            .overview(place.getOverview())
            .chkPetInside(place.getChkPetInside())
            .accomCountPet(place.getAccomCountPet())
            .petTurnAdroose(place.getPetTurnAdroose())
            .homepage(place.getHomepage())
            .createdAt(place.getCreatedAt())
            .updatedAt(place.getUpdatedAt())
            .build();
    }
}
