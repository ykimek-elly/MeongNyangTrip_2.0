package com.team.meongnyang.checkin.repository;

import com.team.meongnyang.checkin.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    /** 내 전체 방문 기록 (최신순) */
    List<CheckIn> findByUser_UserIdOrderByRegDateDesc(Long userId);

    /** 이번 달 방문 횟수 */
    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.user.userId = :userId " +
           "AND c.regDate >= :startOfMonth")
    long countThisMonth(@Param("userId") Long userId,
                        @Param("startOfMonth") LocalDateTime startOfMonth);

    /** 총 방문 횟수 */
    long countByUser_UserId(Long userId);
}
