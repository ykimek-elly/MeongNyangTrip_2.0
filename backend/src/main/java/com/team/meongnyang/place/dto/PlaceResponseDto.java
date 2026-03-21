package com.team.meongnyang.place.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.team.meongnyang.place.entity.Place;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 장소 응답 DTO.
 * Entity의 내부 구조를 직접 노출하지 않고, 필요한 데이터만 가공하여 반환한다.
 *
 * @JsonDeserialize/@JsonPOJOBuilder: Redis 캐시 역직렬화 시 Lombok Builder를 사용하도록 지시.
 * @Builder만 있을 경우 Jackson이 기본 생성자를 찾지 못해 500 에러 발생.
 */
@Getter
@Builder
@JsonDeserialize(builder = PlaceResponseDto.PlaceResponseDtoBuilder.class)
public class PlaceResponseDto {

    @JsonPOJOBuilder(withPrefix = "")
    public static class PlaceResponseDtoBuilder {
        // Lombok이 내용을 채워줌 — Jackson은 이 Builder를 역직렬화에 사용
    }

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
    private Double aiRating;
    private Integer blogCount;
    private String blogPositiveTags;
    private String blogNegativeTags;
    private String petFacility;
    private String petPolicy;
    private String operatingHours;
    private String operationPolicy;
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
            .aiRating(place.getAiRating())
            .blogCount(place.getBlogCount())
            .blogPositiveTags(place.getBlogPositiveTags())
            .blogNegativeTags(place.getBlogNegativeTags())
            .petFacility(place.getPetFacility())
            .petPolicy(place.getPetPolicy())
            .operatingHours(place.getOperatingHours())
            .operationPolicy(place.getOperationPolicy())
            .createdAt(place.getCreatedAt())
            .updatedAt(place.getUpdatedAt())
            .build();
    }
}
