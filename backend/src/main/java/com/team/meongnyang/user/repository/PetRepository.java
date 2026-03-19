package com.team.meongnyang.user.repository;

import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 반려동물 리포지토리
 */
public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByUserUserId(Long userId);

    Optional<Pet> findByUserUserIdAndIsRepresentativeTrue(Long userId);
    Optional<Pet> findFirstByUser (User user);
}
