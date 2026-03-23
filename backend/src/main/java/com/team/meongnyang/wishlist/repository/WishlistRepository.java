package com.team.meongnyang.wishlist.repository;

import com.team.meongnyang.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUser_UserIdAndPlace_Id(Long userId, Long placeId);

    List<Wishlist> findByUser_UserIdOrderByRegDateDesc(Long userId);

    void deleteByUser_UserIdAndPlace_Id(Long userId, Long placeId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.place.id = :placeId")
    void deleteByPlace_Id(Long placeId);
}
