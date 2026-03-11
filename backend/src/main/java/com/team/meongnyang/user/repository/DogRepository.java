package com.team.meongnyang.user.repository;

import com.team.meongnyang.user.entity.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 반려견 리포지토리
 */
public interface DogRepository extends JpaRepository<Dog, Long> {

    List<Dog> findByUserUserId(Long userId);
}
