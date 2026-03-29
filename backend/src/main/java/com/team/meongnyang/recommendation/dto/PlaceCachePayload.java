package com.team.meongnyang.recommendation.dto;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.entity.PlaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceCachePayload {

  private Long id;
  private String contentId;
  private String kakaoId;
  private Integer version;
  private Boolean isVerified;
  private String status;
  private String pendingReason;
  private String title;
  private String description;
  private String address;
  private Double latitude;
  private Double longitude;
  private String category;
  private Double rating;
  private Integer reviewCount;
  private String imageUrl;
  private String phone;
  private String tags;
  private String addr2;
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

  public static PlaceCachePayload from(Place place) {
    if (place == null) {
      return null;
    }

    return PlaceCachePayload.builder()
            .id(place.getId())
            .contentId(place.getContentId())
            .kakaoId(place.getKakaoId())
            .version(place.getVersion())
            .isVerified(place.getIsVerified())
            .status(place.getStatus() == null ? null : place.getStatus().name())
            .pendingReason(place.getPendingReason())
            .title(place.getTitle())
            .description(place.getDescription())
            .address(place.getAddress())
            .latitude(place.getLatitude())
            .longitude(place.getLongitude())
            .category(place.getCategory())
            .rating(place.getRating())
            .reviewCount(place.getReviewCount())
            .imageUrl(place.getImageUrl())
            .phone(place.getPhone())
            .tags(place.getTags())
            .addr2(place.getAddr2())
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
            .build();
  }

  public Place toPlace() {
    return Place.builder()
            .id(id)
            .contentId(contentId)
            .kakaoId(kakaoId)
            .version(version)
            .isVerified(isVerified)
            .status(status == null ? null : PlaceStatus.valueOf(status))
            .pendingReason(pendingReason)
            .title(title)
            .description(description)
            .address(address)
            .latitude(latitude)
            .longitude(longitude)
            .category(category)
            .rating(rating)
            .reviewCount(reviewCount)
            .imageUrl(imageUrl)
            .phone(phone)
            .tags(tags)
            .addr2(addr2)
            .overview(overview)
            .chkPetInside(chkPetInside)
            .accomCountPet(accomCountPet)
            .petTurnAdroose(petTurnAdroose)
            .homepage(homepage)
            .aiRating(aiRating)
            .blogCount(blogCount)
            .blogPositiveTags(blogPositiveTags)
            .blogNegativeTags(blogNegativeTags)
            .petFacility(petFacility)
            .petPolicy(petPolicy)
            .operatingHours(operatingHours)
            .operationPolicy(operationPolicy)
            .build();
  }
}
