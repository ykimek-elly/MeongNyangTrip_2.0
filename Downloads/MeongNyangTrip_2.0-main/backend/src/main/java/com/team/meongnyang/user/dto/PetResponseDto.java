package com.team.meongnyang.user.dto;

import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.Pet.PetActivity;
import com.team.meongnyang.user.entity.Pet.PetGender;
import com.team.meongnyang.user.entity.Pet.PetSize;
import com.team.meongnyang.user.entity.Pet.PetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PetResponseDto {

    private Long petId;
    private String petName;
    private PetType petType;
    private String petBreed;
    private PetGender petGender;
    private PetSize petSize;
    private Integer petAge;
    private BigDecimal petWeight;
    private PetActivity petActivity;
    private String personality;
    private String preferredPlace;
    private Boolean isRepresentative;

    public static PetResponseDto from(Pet pet) {
        return PetResponseDto.builder()
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .petType(pet.getPetType())
                .petBreed(pet.getPetBreed())
                .petGender(pet.getPetGender())
                .petSize(pet.getPetSize())
                .petAge(pet.getPetAge())
                .petWeight(pet.getPetWeight())
                .petActivity(pet.getPetActivity())
                .personality(pet.getPersonality())
                .preferredPlace(pet.getPreferredPlace())
                .isRepresentative(pet.getIsRepresentative())
                .build();
    }
}
