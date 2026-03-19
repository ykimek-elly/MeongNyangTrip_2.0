package com.team.meongnyang.review.entity;

import com.team.meongnyang.common.BaseEntity;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 리뷰(Review) 엔티티.
 * 사용자가 장소에 남긴 별점 + 텍스트 리뷰를 저장한다.
 */
@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 리뷰 본문 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 별점 (1.0 ~ 5.0) */
    @Column(nullable = false)
    private Double rating;

    /** 리뷰 이미지 URL (선택, 추후 S3 연동) */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
}
