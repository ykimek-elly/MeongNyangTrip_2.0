package com.team.meongnyang.user.dto;

import com.team.meongnyang.user.entity.Pet.PetActivity;
import com.team.meongnyang.user.entity.Pet.PetGender;
import com.team.meongnyang.user.entity.Pet.PetSize;
import com.team.meongnyang.user.entity.Pet.PetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PetRequestDto {

    @NotBlank
    private String petName;

    @NotNull
    private PetType petType;

    @NotBlank
    private String petBreed;

    @NotNull
    private PetGender petGender;

    @NotNull
    private PetSize petSize;

    @NotNull
    @Min(0)
    private Integer petAge;

    private BigDecimal petWeight;

    @NotNull
    private PetActivity petActivity;

    private String personality;

    private String preferredPlace;
}
