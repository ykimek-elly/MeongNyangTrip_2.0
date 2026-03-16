package com.team.meongnyang.orchestrator.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.DogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrchDogService {
  private final DogRepository dogRepository;

  /**
   * 사용자의 대표 반려견을 조회한다
   *
   * todo : 현재는 다견 등록과 대표견 1마리까지만 지원
   *
   * @param user 사용자 엔티티
   * @return 대표 반려견 엔티티.
   * @throws IllegalArgumentException 반려견 정보가 없을 경우
   */
  public Dog getPrimaryDog (User user) {
    return dogRepository.findFirstByUser(user)
            .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
  }
}
