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
    private String region;
    private Integer activityRadius;
    private Boolean isRepresentative;
    private Boolean notifyEnabled;

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
                .region(pet.getRegion())
                .activityRadius(pet.getActivityRadius())
                .isRepresentative(pet.getIsRepresentative())
                .notifyEnabled(pet.getNotifyEnabled())
                .build();
    }
}
