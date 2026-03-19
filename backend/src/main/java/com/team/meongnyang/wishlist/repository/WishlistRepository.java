package com.team.meongnyang.wishlist.repository;

import com.team.meongnyang.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUser_UserIdAndPlace_Id(Long userId, Long placeId);

    List<Wishlist> findByUser_UserIdOrderByRegDateDesc(Long userId);

    void deleteByUser_UserIdAndPlace_Id(Long userId, Long placeId);
}
