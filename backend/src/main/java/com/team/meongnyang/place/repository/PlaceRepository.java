package com.team.meongnyang.place.repository;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.entity.PlaceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 장소(Place) JPA Repository.
 * 기본 CRUD + 카테고리/키워드 검색 + PostGIS 위치 기반 검색.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /** 공공데이터 원본 ID로 단건 조회 (배치 Upsert 기준) */
    Optional<Place> findByContentId(String contentId);

    /** 카카오 장소 ID로 단건 조회 (데이터셋 간 중복 방지 dedup 기준) */
    Optional<Place> findByKakaoId(String kakaoId);

    /** 교차검증 대상 조회 (isVerified=false인 레코드) */
    List<Place> findByIsVerified(boolean isVerified);

    /** 이미지 단독 보강 대상 조회 — 이미지 없고 폐업 아닌 장소만 (enrich 완료 후 실행) */
    @Query("SELECT p FROM Place p WHERE (p.imageUrl IS NULL OR p.imageUrl = '') AND (p.tags IS NULL OR p.tags NOT LIKE '%폐업%')")
    List<Place> findByImageUrlIsNullOrEmpty();

    /** Gemini 이미지 검증 대상 조회 — 이미지 있고 폐업 아닌 장소 */
    @Query("SELECT p FROM Place p WHERE p.imageUrl IS NOT NULL AND p.imageUrl <> '' AND (p.tags IS NULL OR p.tags NOT LIKE '%폐업%')")
    List<Place> findByImageUrlIsNotNullAndNotEmpty();

    /** 깨진 이미지 URL 대상 조회 — Instagram/Facebook/Pinterest CDN (핫링크 차단 또는 403) */
    @Query("SELECT p FROM Place p WHERE p.imageUrl LIKE '%cdninstagram%' OR p.imageUrl LIKE '%fbcdn%' OR p.imageUrl LIKE '%pinimg%' OR p.imageUrl LIKE '%pinterest%'")
    List<Place> findByBrokenImageUrls();

    /** 깨진 이미지 URL NULL 초기화 — 배치 재실행 전처리 (Instagram/Facebook/Pinterest) */
    @Modifying
    @Query("UPDATE Place p SET p.imageUrl = NULL WHERE p.imageUrl LIKE '%cdninstagram%' OR p.imageUrl LIKE '%fbcdn%' OR p.imageUrl LIKE '%pinimg%' OR p.imageUrl LIKE '%pinterest%'")
    int resetBrokenImageUrls();

    /** 뉴스 CDN 이미지 NULL 초기화 — imgnews.naver.net 오배치 교체용 */
    @Modifying
    @Query("UPDATE Place p SET p.imageUrl = NULL WHERE p.imageUrl LIKE '%imgnews.naver.net%'")
    int resetNewsImageUrls();

    /** 교차검증 플래그 전체 리셋 — enrich 배치 재실행 전처리 */
    @Modifying
    @Query("UPDATE Place p SET p.isVerified = false")
    int resetAllVerified();

    /** AI 별점 재계산 대상 조회 — aiRating이 null인 장소 (폐업 제외) */
    @Query("SELECT p FROM Place p WHERE p.aiRating IS NULL AND (p.tags IS NULL OR p.tags NOT LIKE '%폐업%')")
    List<Place> findByAiRatingIsNull();

    /** 상태별 장소 조회 — PENDING(관리자 검토 큐) 등 */
    List<Place> findByStatusOrderByCreatedAtDesc(PlaceStatus status);

    /** 카테고리별 장소 목록 조회 */
    List<Place> findByCategory(String category);

    /** 제목 키워드 검색 (대소문자 무시) */
    List<Place> findByTitleContainingIgnoreCase(String keyword);

    /** 카테고리 + 키워드 복합 검색 */
    List<Place> findByCategoryAndTitleContainingIgnoreCase(String category, String keyword);

    /**
     * PostGIS ST_DWithin — 위경도 기준 반경 내 장소 검색 (거리 오름차순)
     * @param lat    사용자 위도
     * @param lng    사용자 경도
     * @param radius 검색 반경 (미터)
     * @param limit  최대 반환 수
     */
    @Query(value = """
        SELECT * FROM places p
        WHERE p.geom IS NOT NULL
          AND ST_DWithin(
            p.geom::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radius
          )
        ORDER BY ST_Distance(
            p.geom::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Place> findNearby(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radius") int radius,
        @Param("limit") int limit
    );

    /**
     * PostGIS ST_DWithin — 카테고리 필터 포함 반경 검색
     */
    @Query(value = """
        SELECT * FROM places p
        WHERE p.geom IS NOT NULL
          AND p.category = :category
          AND ST_DWithin(
            p.geom::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radius
          )
        ORDER BY ST_Distance(
            p.geom::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Place> findNearbyByCategory(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radius") int radius,
        @Param("category") String category,
        @Param("limit") int limit
    );

}
