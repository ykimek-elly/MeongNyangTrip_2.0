package com.team.meongnyang.review.repository;

import com.team.meongnyang.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPlace_IdOrderByRegDateDesc(Long placeId);

    List<Review> findByUser_UserIdOrderByRegDateDesc(Long userId);

    Optional<Review> findByUser_UserIdAndPlace_Id(Long userId, Long placeId);

    boolean existsByUser_UserIdAndPlace_Id(Long userId, Long placeId);

    long countByPlace_Id(Long placeId);
}
