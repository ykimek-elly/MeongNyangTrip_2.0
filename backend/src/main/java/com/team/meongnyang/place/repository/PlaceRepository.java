package com.team.meongnyang.place.repository;

import com.team.meongnyang.place.entity.Place;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 장소(Place) JPA Repository.
 * 기본 CRUD + 카테고리별 조회, 키워드 검색을 제공한다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /** 카테고리별 장소 목록 조회 */
    List<Place> findByCategory(String category);

    /** 제목 키워드 검색 (대소문자 무시) */
    List<Place> findByTitleContainingIgnoreCase(String keyword);

    /** 카테고리 + 키워드 복합 검색 */
    List<Place> findByCategoryAndTitleContainingIgnoreCase(String category, String keyword);


    List<Place> findByTagsContaining(String tag, Pageable pageable);

}
