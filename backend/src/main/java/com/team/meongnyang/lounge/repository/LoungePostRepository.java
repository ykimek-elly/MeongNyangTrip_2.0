package com.team.meongnyang.lounge.repository;

import com.team.meongnyang.lounge.entity.LoungePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoungePostRepository extends JpaRepository<LoungePost, Long> {

    @Query("SELECT p FROM LoungePost p WHERE p.isHidden = false ORDER BY p.createdAt DESC")
    List<LoungePost> findAllVisibleOrderByCreatedAtDesc();
}
