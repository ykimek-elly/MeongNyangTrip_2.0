package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationUserReader {

  private final UserRepository userRepository;

  /**
   * 사용자 조회
   *  - 이메일로 사용자를 조회한다
   *  - 사용자가 없으면 에러를 반환한다
   *  - 사용자가 있으면 사용자 정보를 반환한다
   * @param email 사용자 이메일
   * @return 사용자 정보
   */
  public User getCurrentUserByEmail(String email) {
    User user = userRepository.findByEmail(email)
                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    log.info("[RecommendationUserReader] 사용자 조회 완료 email={}, userId={}, nickname={}", email, user.getUserId(), user.getNickname());
    return user;
  }
}
