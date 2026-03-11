package com.team.meongnyang.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 등록/수정 요청 DTO.
 * Entity 직접 노출을 방지하고, 유효성 검증을 수행한다.
 */
@Getter
@NoArgsConstructor
public class PlaceRequestDto {

    @NotBlank(message = "장소명은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;

    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    private String imageUrl;

    private String phone;

    private String tags;
}
