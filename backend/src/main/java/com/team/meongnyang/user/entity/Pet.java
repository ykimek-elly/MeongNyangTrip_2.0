package com.team.meongnyang.user.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * 반려동물 통합 엔티티 (ERD: PET)
 */
@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Pet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long petId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(min = 1, max = 20)
    @Column(name = "pet_name", nullable = false, length = 20)
    private String petName;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false, length = 10)
    private PetType petType;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(name = "pet_breed", nullable = false, length = 50)
    private String petBreed;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_gender", nullable = false, length = 10)
    private PetGender petGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_size", nullable = false, length = 10)
    private PetSize petSize;

    @NotNull
    @Min(0)
    @Column(name = "pet_age", nullable = false)
    private Integer petAge;

    @Column(name = "pet_weight", precision = 5, scale = 2)
    private BigDecimal petWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_activity", nullable = false, length = 10)
    private PetActivity petActivity;

    @Size(max = 100)
    @Column(length = 100)
    private String personality;

    @Size(max = 50)
    @Column(name = "preferred_place", length = 50)
    private String preferredPlace;

    @Size(max = 50)
    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "activity_radius")
    private Integer activityRadius;

    @Column(name = "is_representative", nullable = false)
    @Builder.Default
    private Boolean isRepresentative = false;

    @Column(name = "notify_enabled", nullable = false)
    @Builder.Default
    private Boolean notifyEnabled = true;

    public enum PetType {
        강아지, 고양이
    }

    public enum PetGender {
        남아, 여아
    }

    public enum PetSize {
        SMALL, MEDIUM, LARGE
    }

    public enum PetActivity {
        LOW, NORMAL, HIGH
    }

    public void update(String petName, PetType petType, String petBreed, PetGender petGender,
                       PetSize petSize, Integer petAge, BigDecimal petWeight,
                       PetActivity petActivity, String personality, String preferredPlace,
                       String region, Integer activityRadius, Boolean notifyEnabled) {
        this.petName = petName;
        this.petType = petType;
        this.petBreed = petBreed;
        this.petGender = petGender;
        this.petSize = petSize;
        this.petAge = petAge;
        this.petWeight = petWeight;
        this.petActivity = petActivity;
        this.personality = personality;
        this.preferredPlace = preferredPlace;
        this.region = region;
        this.activityRadius = activityRadius;
        this.notifyEnabled = notifyEnabled != null ? notifyEnabled : true;
    }

    public void setRepresentative(boolean isRepresentative) {
        this.isRepresentative = isRepresentative;
    }
}
