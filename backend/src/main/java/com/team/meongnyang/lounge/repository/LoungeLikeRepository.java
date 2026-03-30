package com.team.meongnyang.lounge.repository;

import com.team.meongnyang.lounge.entity.LoungeLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoungeLikeRepository extends JpaRepository<LoungeLike, Long> {
    Optional<LoungeLike> findByPost_PostIdAndUser_Email(Long postId, String email);
    boolean existsByPost_PostIdAndUser_Email(Long postId, String email);
}
