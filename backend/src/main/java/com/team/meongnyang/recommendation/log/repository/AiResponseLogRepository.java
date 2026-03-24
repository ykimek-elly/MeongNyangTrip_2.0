package com.team.meongnyang.recommendation.log.repository;

import com.team.meongnyang.recommendation.log.entity.AiResponseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiResponseLogRepository extends JpaRepository<AiResponseLog, Long> {
  List<AiResponseLog> findByUserIdAndRegDateAfterOrderByRegDateDesc(Long userId, LocalDateTime regDate);
}
