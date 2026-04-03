package com.team.meongnyang.friend.repository;

import com.team.meongnyang.friend.entity.Friend;
import com.team.meongnyang.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUserUserId(Long userId);
    List<Friend> findByUser(User user);
    Optional<Friend> findByUserAndFriendUser(User user, User friendUser);
    boolean existsByUserAndFriendUser(User user, User friendUser);
}
