package com.team.meongnyang.pettour.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.pettour.service.PetTourApiService;
import com.team.meongnyang.place.dto.PlaceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 프론트엔드를 위한 공공API 프록시 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/public-places")
@RequiredArgsConstructor
public class PetTourController {

    private final PetTourApiService petTourApiService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponseDto>>> getPublicPlaces(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int numOfRows
    ) {
        List<PlaceResponseDto> places = petTourApiService.getPetTourList(pageNo, numOfRows);
        return ResponseEntity.ok(
                ApiResponse.success("공공 API 장소 목록 조회 성공", places)
        );
    }
}
