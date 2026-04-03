package com.team.meongnyang.recommendation.dto;

import com.team.meongnyang.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationLookupResponse {

    private Long userId;
    private Long petId;
    private String petName;
    private String weatherType;
    private String weatherWalkLevel;
    private String weatherSummary;
    private Place place;
    private String notificationSummary;
    private String recommendationDescription;
    private boolean fallbackUsed;
    private boolean cacheHit;
    private boolean error;
    private String errorCode;
}
