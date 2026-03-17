package com.team.meongnyang.pettour.service;

import com.team.meongnyang.pettour.dto.PetTourApiResponse;
import com.team.meongnyang.place.dto.PlaceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국관광공사 반려동물 동반여행 API 연동 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetTourApiService {

    private final RestClient restClient;

    @Value("${pet-tour.service-key}")
    private String serviceKey;

    @Value("${pet-tour.base-url}")
    private String baseUrl;

    public List<PlaceResponseDto> getPetTourList(int pageNo, int numOfRows) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/areaBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "MeongNyangTrip")
                .queryParam("_type", "json")
                .build(true) // 인코딩된 서비스 키가 다시 인코딩되지 않도록 true 설정
                .toUri();

        log.info("Requesting Pet Tour API: {}", uri);

        PetTourApiResponse apiResponse = restClient.get()
                .uri(uri)
                .retrieve()
                .body(PetTourApiResponse.class);

        return mapToPlaceResponseDtoList(apiResponse);
    }

    private List<PlaceResponseDto> mapToPlaceResponseDtoList(PetTourApiResponse apiResponse) {
        List<PlaceResponseDto> resultList = new ArrayList<>();

        if (apiResponse != null && apiResponse.getResponse() != null 
            && apiResponse.getResponse().getBody() != null
            && apiResponse.getResponse().getBody().getItems() != null
            && apiResponse.getResponse().getBody().getItems().getItem() != null) {

            for (PetTourApiResponse.Item item : apiResponse.getResponse().getBody().getItems().getItem()) {
                
                try {
                    PlaceResponseDto dto = PlaceResponseDto.builder()
                            .id(Long.parseLong(item.getContentid()))
                            .title(item.getTitle())
                            .address(item.getAddr1())
                            .latitude(item.getMapy() != null && !item.getMapy().isEmpty() ? Double.parseDouble(item.getMapy()) : 0.0)
                            .longitude(item.getMapx() != null && !item.getMapx().isEmpty() ? Double.parseDouble(item.getMapx()) : 0.0)
                            .imageUrl(item.getFirstimage())
                            .phone(item.getTel())
                            .category(determineCategory(item.getContenttypeid()))
                            .rating(4.5) // 임시 더미 평점
                            .reviewCount(0)
                            .description("공공데이터 제공 정보입니다.")
                            .build();
                    resultList.add(dto);
                } catch (Exception e) {
                    log.error("Failed to parse item contentid: {}", item.getContentid(), e);
                }
            }
        }
        return resultList;
    }
    
    // contenttypeid 별 카테고리 매핑 (임시)
    private String determineCategory(String contentTypeId) {
        if ("32".equals(contentTypeId)) return "STAY";
        if ("39".equals(contentTypeId)) return "DINING";
        return "PLACE";
    }
}
