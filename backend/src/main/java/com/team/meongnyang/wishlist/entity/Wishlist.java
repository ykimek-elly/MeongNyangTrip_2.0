package com.team.meongnyang.wishlist.entity;

import com.team.meongnyang.common.BaseEntity;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 찜하기(Wishlist) 엔티티.
 * 사용자가 장소를 찜(북마크)한 정보를 저장한다.
 */
@Entity
@Table(name = "wishlists",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "place_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Long wishlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;
}
