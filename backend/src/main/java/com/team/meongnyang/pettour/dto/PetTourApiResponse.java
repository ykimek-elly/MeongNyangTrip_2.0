package com.team.meongnyang.pettour.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 한국관광공사 반려동물 동반여행 API 응답 매핑용 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class PetTourApiResponse {

    private Response response;

    @Getter
    @Setter
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    public static class Body {
        private Items items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
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
        private String title;
        private String addr1;
        private String addr2;
        private String mapx;
        private String mapy;
        private String firstimage;
        private String tel;
        private String contenttypeid;
    }
}
