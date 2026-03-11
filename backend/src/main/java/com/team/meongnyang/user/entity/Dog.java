package com.team.meongnyang.user.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 반려견 엔티티 (ERD: DOG)
 */
@Entity
@Table(name = "dogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Dog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dog_id")
    private Long dogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(min = 1, max = 20)
    @Column(name = "dog_name", nullable = false, length = 20)
    private String dogName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dog_size", nullable = false, length = 10)
    private DogSize dogSize;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(name = "dog_breed", nullable = false, length = 50)
    private String dogBreed;

    @Size(max = 100)
    @Column(length = 100)
    private String personality;

    @Size(max = 50)
    @Column(name = "preferred_place", length = 50)
    private String preferredPlace;

    public enum DogSize {
        SMALL, MEDIUM, LARGE
    }
}
