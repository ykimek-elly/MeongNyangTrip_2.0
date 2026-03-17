package com.team.meongnyang.pettour.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 한국관광공사 공통 상세정보 API (detailCommon2) 응답 매핑용 DTO
 * overview(소개글) 수신 목적
 */
@Getter
@Setter
@NoArgsConstructor
public class DetailCommonResponse {

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
        /** 장소 소개 */
        private String overview;
        /** 홈페이지 URL */
        private String homepage;
    }
}
