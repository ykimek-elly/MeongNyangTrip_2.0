package com.team.meongnyang.user.repository;

import com.team.meongnyang.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 회원 리포지토리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** 소셜 로그인 — 제공자 + 제공자 고유 ID로 조회 */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     *  NotificationEnalbed 가 true이면서 Status 에 맞는 사용자 목록을 조회한다.
     */
    List<User> findAllByNotificationEnabledTrueAndStatus(User.Status status);
}
