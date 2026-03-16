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
   * todo : 현재는 다견 등록과 대표견 1마리까지만 지원
   *
   * @param user 사용자 엔티티
   * @return 대표 반려견 엔티티.
   * @throws IllegalArgumentException 반려견 정보가 없을 경우
   */
  public Pet getPrimaryPet(User user) {
    Pet pet = petRepository.findFirstByUser(user)
            .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    log.info("[OrchPetService] 대표 반려견 조회 완료 userId={}, petId={}, name={}, representative={}",
            user.getUserId(),
            pet.getPetId(),
            pet.getPetName(),
            pet.getIsRepresentative());
    return pet;
  }


}
