package com.team.meongnyang.dm.repository;

import com.team.meongnyang.dm.entity.DmMessage;
import com.team.meongnyang.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

    @Query("SELECT m FROM DmMessage m WHERE " +
           "(m.sender = :user AND m.receiver = :partner) OR " +
           "(m.sender = :partner AND m.receiver = :user) " +
           "ORDER BY m.regDate ASC")
    List<DmMessage> findConversation(@Param("user") User user, @Param("partner") User partner);

    @Query(value = "SELECT * FROM dm_messages m WHERE m.id IN (" +
           "  SELECT MAX(id) FROM dm_messages m2 " +
           "  WHERE m2.sender_id = :userId OR m2.receiver_id = :userId " +
           "  GROUP BY CASE WHEN m2.sender_id = :userId THEN m2.receiver_id ELSE m2.sender_id END" +
           ") ORDER BY m.reg_date DESC", nativeQuery = true)
    List<DmMessage> findLatestMessagesForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM DmMessage m WHERE m.receiver = :user AND m.sender = :partner AND m.isRead = false")
    long countUnreadMessages(@Param("user") User user, @Param("partner") User partner);
}
