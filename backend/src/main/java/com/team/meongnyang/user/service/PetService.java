package com.team.meongnyang.user.service;

import com.team.meongnyang.user.dto.PetRequestDto;
import com.team.meongnyang.user.dto.PetResponseDto;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PetResponseDto> getPetsByUserId(Long userId) {
        return petRepository.findByUserUserId(userId)
                .stream()
                .map(PetResponseDto::from)
                .toList();
    }

    @Transactional
    public PetResponseDto addPet(Long userId, PetRequestDto request) {
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        boolean hasNoPets = petRepository.findByUserUserId(userId).isEmpty();

        Pet pet = Pet.builder()
                .user(user)
                .petName(request.getPetName())
                .petType(request.getPetType())
                .petBreed(request.getPetBreed())
                .petGender(request.getPetGender())
                .petSize(request.getPetSize())
                .petAge(request.getPetAge())
                .petWeight(request.getPetWeight())
                .petActivity(request.getPetActivity())
                .personality(request.getPersonality())
                .preferredPlace(request.getPreferredPlace())
                .region(request.getRegion())
                .activityRadius(request.getActivityRadius())
                .isRepresentative(hasNoPets)
                .build();

        @SuppressWarnings("null")
        Pet savedPet = petRepository.save(pet);
        return PetResponseDto.from(savedPet);
    }

    @Transactional
    public PetResponseDto updatePet(Long petId, Long userId, PetRequestDto request) {
        Pet pet = findOwnedPet(petId, userId);
        pet.update(
                request.getPetName(), request.getPetType(), request.getPetBreed(),
                request.getPetGender(), request.getPetSize(), request.getPetAge(),
                request.getPetWeight(), request.getPetActivity(),
                request.getPersonality(), request.getPreferredPlace(),
                request.getRegion(), request.getActivityRadius()
        );
        return PetResponseDto.from(pet);
    }

    @Transactional
    public void deletePet(Long petId, Long userId) {
        Pet pet = findOwnedPet(petId, userId);
        boolean wasRepresentative = Boolean.TRUE.equals(pet.getIsRepresentative());
        petRepository.delete(pet);

        // 대표 반려동물 삭제 시 다음 반려동물 자동 승격
        if (wasRepresentative) {
            petRepository.findByUserUserId(userId).stream()
                    .findFirst()
                    .ifPresent(next -> next.setRepresentative(true));
        }
    }

    @Transactional
    public PetResponseDto setRepresentative(Long petId, Long userId) {
        // 기존 대표 해제
        petRepository.findByUserUserIdAndIsRepresentativeTrue(userId)
                .ifPresent(prev -> prev.setRepresentative(false));

        // 새 대표 설정
        Pet pet = findOwnedPet(petId, userId);
        pet.setRepresentative(true);
        return PetResponseDto.from(pet);
    }

    private Pet findOwnedPet(Long petId, Long userId) {
        @SuppressWarnings("null")
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다."));
        if (!pet.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return pet;
    }
}
