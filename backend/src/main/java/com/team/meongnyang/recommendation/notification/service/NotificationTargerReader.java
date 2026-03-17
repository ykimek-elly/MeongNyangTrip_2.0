package com.team.meongnyang.recommendation.notification.service;


import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationTargerReader {
  private final UserRepository userRepository;

  public List<User> getNotificationTargets() {
    List<User> targets = userRepository.findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE);
    log.info("[알림 대상 사용자 조회] 알림 수신 대상자 조회 완료, {}명", targets);
    return targets;
  }
}
