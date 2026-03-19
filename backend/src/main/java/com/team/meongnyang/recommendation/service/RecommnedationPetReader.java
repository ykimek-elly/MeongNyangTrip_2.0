package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommnedationPetReader {
  private final PetRepository petRepository;

  /**
   * 사용자의 대표 반려견을 조회한다
   *
   * todo : 대표 펫이 없다면 FirstPet 조회 그래도 없으면 Fallback 전략
   *
   *
   * @param user 사용자 엔티티
   * @return 대표 반려견 엔티티.
   * @throws IllegalArgumentException 반려견 정보가 없을 경우
   */
  public Pet getPrimaryPet(User user) {
    Pet pet = petRepository.findByUserUserIdAndIsRepresentativeTrue(user.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    log.info("[OrchPetService] 대표 반려견 조회 완료 userId={}, petId={}, name={}, representative={}",
            user.getUserId(),
            pet.getPetId(),
            pet.getPetName(),
            pet.getIsRepresentative());
    return pet;
  }


}
