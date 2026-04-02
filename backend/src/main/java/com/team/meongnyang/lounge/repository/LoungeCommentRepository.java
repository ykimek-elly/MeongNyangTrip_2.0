package com.team.meongnyang.lounge.repository;

import com.team.meongnyang.lounge.entity.LoungeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoungeCommentRepository extends JpaRepository<LoungeComment, Long> {
    List<LoungeComment> findByPost_PostIdOrderByCommentIdAsc(Long postId);
}