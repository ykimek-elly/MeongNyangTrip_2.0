package com.team.meongnyang.orchestrator.service;

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
public class OrchUserService {

  private final UserRepository userRepository;


  /*
    todo : 사용자가 로그인한다
     1. 서버가 회원 고유 id 또는 email/providerId 를 인증정보에 저장한다
     2. 이후 API에서는 클라이언트가 userId를 넘기지 않는다
     3. 서버가 인증정보에서 직접 현재 사용자 식별값을 꺼내 User를 조회한다
   */

  public User getUserById (Long id) {
    return userRepository.findByUserId(id)
                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }
}
