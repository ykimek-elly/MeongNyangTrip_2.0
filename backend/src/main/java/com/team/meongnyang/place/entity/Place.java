package com.team.meongnyang.place.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

/**
 * 장소(Place) JPA Entity.
 * 반려동물 동반 가능한 플레이스, 스테이, 다이닝 등의 장소 정보를 관리한다.
 */
@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공공데이터 원본 ID (upsert 기준키) */
    @Column(name = "content_id", unique = true)
    private String contentId;

    /** PostGIS 공간 좌표 (SRID:4326) — ST_DWithin 검색용 */
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geom;

    /** Optimistic Lock — 리뷰/평점 동시 갱신 제어 */
    @Version
    private Integer version;

    /** Kakao Local API 교차검증 통과 여부 */
    @Column(name = "is_verified", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private Boolean isVerified = false;

    /** 장소명 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 장소 설명 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 주소 */
    @Column(nullable = false)
    private String address;

    /** 위도 (PostGIS 공간 쿼리용) */
    @Column(nullable = false)
    private Double latitude;

    /** 경도 (PostGIS 공간 쿼리용) */
    @Column(nullable = false)
    private Double longitude;

    /** 카테고리 (PLACE, STAY, DINING) */
    @Column(nullable = false, length = 20)
    private String category;

    /** 평균 평점 */
    @Column(nullable = false)
    @Builder.Default
    private Double rating = 0.0;

    /** 리뷰 수 */
    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    /** 대표 이미지 URL */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** 연락처 */
    private String phone;

    /** 반려동물 관련 태그 (JSON 문자열: ["대형견가능", "주차가능"]) */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /** 상세 주소 (건물명 등) — 공공데이터 addr2 */
    private String addr2;

    /** 장소 소개 (공공데이터 detailCommon2 overview) */
    @Column(columnDefinition = "TEXT")
    private String overview;

    /** 실내 반려동물 동반 가능 여부 (Y/N) — detailPetTour2 */
    @Column(name = "chk_pet_inside", length = 10)
    private String chkPetInside;

    /** 반려동물 동반 수용 가능 수 — detailPetTour2 */
    @Column(name = "accom_count_pet", length = 50)
    private String accomCountPet;

    /** 반려동물 동반 관광 상세정보 — detailPetTour2 */
    @Column(name = "pet_turn_adroose", columnDefinition = "TEXT")
    private String petTurnAdroose;

    /** 홈페이지 URL — detailCommon2 */
    @Column(columnDefinition = "TEXT")
    private String homepage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 장소 정보 수정 (관리자용) */
    public void update(String title, String description, String address,
                       Double latitude, Double longitude, String category,
                       String imageUrl, String phone, String tags) {
        this.title = title;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.imageUrl = imageUrl;
        this.phone = phone;
        this.tags = tags;
    }

    /**
     * Google Places API 속성 보강 (3단계 교차검증).
     * allowsDogs=true 시 tags에 "반려동물 동반 가능" 추가.
     * isClosed=true 시 tags에 "폐업" 추가.
     * 실행 후 isVerified=true 처리.
     */
    public void enrichFromGoogle(boolean allowsDogs, boolean isClosed) {
        if (allowsDogs) {
            this.tags = (this.tags != null && !this.tags.isBlank())
                    ? this.tags + ",반려동물 동반 가능"
                    : "반려동물 동반 가능";
        }
        if (isClosed) {
            this.tags = (this.tags != null && !this.tags.isBlank())
                    ? this.tags + ",폐업"
                    : "폐업";
        }
        this.isVerified = true;
    }

    /** 네이버 지역 검색 API 이미지 보강 */
    public void enrichImageFromNaver(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** 배치 Upsert — 공공데이터 + Kakao 교차검증 결과 반영 */
    public void upsertFromBatch(String title, String address, String addr2,
                                Double latitude, Double longitude,
                                Point geom, String category, String imageUrl, String phone,
                                String overview, String homepage,
                                String chkPetInside, String accomCountPet,
                                String petTurnAdroose, boolean isVerified) {
        this.title = title;
        this.address = address;
        this.addr2 = addr2;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geom = geom;
        this.category = category;
        this.imageUrl = imageUrl;
        this.phone = phone;
        this.overview = overview;
        this.homepage = homepage;
        this.chkPetInside = chkPetInside;
        this.accomCountPet = accomCountPet;
        this.petTurnAdroose = petTurnAdroose;
        this.isVerified = isVerified;
    }
}
