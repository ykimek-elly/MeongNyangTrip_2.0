package com.team.meongnyang.friend.repository;

import com.team.meongnyang.friend.entity.ShareRecord;
import com.team.meongnyang.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareRecordRepository extends JpaRepository<ShareRecord, Long> {
    List<ShareRecord> findBySender(User sender);
    List<ShareRecord> findByReceiver(User receiver);
}
