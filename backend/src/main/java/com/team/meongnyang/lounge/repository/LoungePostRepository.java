package com.team.meongnyang.lounge.repository;

import com.team.meongnyang.lounge.entity.LoungePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoungePostRepository extends JpaRepository<LoungePost, Long> {
    List<LoungePost> findByIsHiddenFalseOrderByPostIdDesc();
    List<LoungePost> findByIsHiddenFalseAndPostTypeOrderByPostIdDesc(String postType);
    List<LoungePost> findByIsHiddenFalseAndPostTypeAndRegDateAfterOrderByPostIdDesc(String postType, LocalDateTime after);
}