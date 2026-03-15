package com.team.meongnyang.pettour.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 한국관광공사 반려동물 동반여행 상세정보 API (detailPetTour2) 응답 매핑용 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class DetailPetTourResponse {

    private Response response;

    @Getter
    @Setter
    public static class Response {
        private Body body;
    }

    @Getter
    @Setter
    public static class Body {
        private Items items;
    }

    @Getter
    @Setter
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @Setter
    public static class Item {
        private String contentid;
        /** 실내 반려동물 동반 가능 여부 (Y/N) */
        private String chkpetinside;
        /** 반려동물 동반 수용 가능 수 */
        private String accomcountpet;
        /** 반려동물 동반 관광 상세정보 */
        private String petturnadroose;
    }
}
