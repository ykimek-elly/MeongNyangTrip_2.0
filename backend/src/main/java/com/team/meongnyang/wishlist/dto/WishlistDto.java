package com.team.meongnyang.wishlist.dto;

import com.team.meongnyang.wishlist.entity.Wishlist;
import lombok.Getter;

public class WishlistDto {

    /** 찜 토글 응답 */
    @Getter
    public static class ToggleResponse {
        private final boolean wishlisted;
        private final Long placeId;

        public ToggleResponse(boolean wishlisted, Long placeId) {
            this.wishlisted = wishlisted;
            this.placeId = placeId;
        }
    }

    /** 찜 목록 단건 응답 */
    @Getter
    public static class Response {
        private final Long wishlistId;
        private final Long placeId;
        private final String title;
        private final String category;
        private final String imageUrl;
        private final String address;
        private final Double rating;
        private final Integer reviewCount;

        public Response(Wishlist w) {
            this.wishlistId  = w.getWishlistId();
            this.placeId     = w.getPlace().getId();
            this.title       = w.getPlace().getTitle();
            this.category    = w.getPlace().getCategory();
            this.imageUrl    = w.getPlace().getImageUrl();
            this.address     = w.getPlace().getAddress();
            this.rating      = w.getPlace().getRating();
            this.reviewCount = w.getPlace().getReviewCount();
        }
    }
}
