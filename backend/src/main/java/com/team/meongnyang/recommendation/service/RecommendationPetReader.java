package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationPetReader {
  private final PetRepository petRepository;

  /**
   * 사용자의 대표 반려견을 조회한다
   *
   * @param user 사용자 엔티티
   * @return 대표 반려견 엔티티.
   * @throws IllegalArgumentException 반려견 정보가 없을 경우
   */
  public Pet getPrimaryPet(User user) {
    return petRepository.findByUserUserIdAndIsRepresentativeTrue(user.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
  }
}
