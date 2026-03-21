package com.team.meongnyang.place.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

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

    /** 카카오 장소 ID — 데이터셋 간 중복 방지 dedup 키 (nullable) */
    @Column(name = "kakao_id", unique = true)
    private String kakaoId;

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

    /**
     * 장소 상태.
     * ACTIVE: 노출, PENDING: 관리자 검토 대기(유사도 50~79%), REJECTED: 폐기
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlaceStatus status = PlaceStatus.ACTIVE;

    /**
     * 보류 사유 — Stage2 검증 결과 저장 (JSON 문자열)
     * 예) {"similarity":65,"sourceTitle":"개들의수다","kakaoTitle":"샌드피피","kakaoLat":37.123,"kakaoLng":126.456}
     */
    @Column(name = "pending_reason", columnDefinition = "TEXT")
    private String pendingReason;

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

    /** AI 추천 별점 (데이터 품질 + 화제성 + 감성 기반 자동 계산, 0.0~5.0) */
    @Column(name = "ai_rating")
    private Double aiRating;

    /** 네이버 블로그 검색 결과 수 (aiRating 계산 시점 기준) */
    @Column(name = "blog_count")
    private Integer blogCount;

    /** 블로그 감성 분석 — 긍정 키워드 (쉼표 구분 문자열, nullable) */
    @Column(name = "blog_positive_tags", length = 200)
    private String blogPositiveTags;

    /** 블로그 감성 분석 — 부정 키워드 (쉼표 구분 문자열, nullable) */
    @Column(name = "blog_negative_tags", length = 200)
    private String blogNegativeTags;

    /** 반려시설 정보 (AI 보강) */
    @Column(name = "pet_facility", columnDefinition = "TEXT")
    private String petFacility;

    /** 반려정책 (AI 보강) */
    @Column(name = "pet_policy", columnDefinition = "TEXT")
    private String petPolicy;

    /** 운영시간 (AI 보강) */
    @Column(name = "operating_hours", columnDefinition = "TEXT")
    private String operatingHours;

    /** 일반 운영정책 (AI 보강) */
    @Column(name = "operation_policy", columnDefinition = "TEXT")
    private String operationPolicy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Stage2: 유사도 50~79% → 관리자 검토 큐로 이동 */
    public void markPending(String reason) {
        this.status = PlaceStatus.PENDING;
        this.pendingReason = reason;
        this.isVerified = false;
    }

    /** 관리자 승인 — PENDING → ACTIVE */
    public void approveFromPending(Double lat, Double lng, Point geom) {
        if (lat != null) this.latitude = lat;
        if (lng != null) this.longitude = lng;
        if (geom != null) this.geom = geom;
        this.status = PlaceStatus.ACTIVE;
        this.pendingReason = null;
        this.isVerified = true;
    }

    /** 관리자 거절 — PENDING → REJECTED */
    public void rejectFromPending() {
        this.status = PlaceStatus.REJECTED;
        this.isVerified = false;
    }

    /** 재검증 배치 — 네이버/카카오 미검색 → REJECTED */
    public void markRejectedByBatch() {
        this.status = PlaceStatus.REJECTED;
        this.isVerified = false;
    }

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

    /** 관리자 필드 부분 수정 — null은 기존값 유지 */
    public void patchAdminFields(String title, String address, String phone,
                                 String homepage, String imageUrl) {
        if (title != null && !title.isBlank())   this.title    = title;
        if (address != null && !address.isBlank()) this.address  = address;
        if (phone != null)     this.phone    = phone;
        if (homepage != null)  this.homepage = homepage;
        if (imageUrl != null)  this.imageUrl = imageUrl;
    }

    /** 오프라인 AI 보강 데이터 업데이트 */
    public void updateEnrichedData(String overview, String petFacility, String petPolicy,
                                   String operatingHours, String operationPolicy) {
        if (overview != null) this.overview = overview;
        if (petFacility != null) this.petFacility = petFacility;
        if (petPolicy != null) this.petPolicy = petPolicy;
        if (operatingHours != null) this.operatingHours = operatingHours;
        if (operationPolicy != null) this.operationPolicy = operationPolicy;
    }

    /**
     * 네이버+카카오 교차검증 완료 처리.
     * isClosed=true 시 tags에 "폐업" 추가.
     * 실행 후 isVerified=true 처리.
     */
    public void markVerified(boolean isClosed) {
        if (isClosed) {
            this.tags = (this.tags != null && !this.tags.isBlank())
                    ? this.tags + ",폐업"
                    : "폐업";
        }
        this.isVerified = true;
    }

    /**
     * AI 추천 별점 자동 계산 — Rubric V3.0 (5.0점 만점)
     * PlaceEnrichBatchService에서 markVerified() 직후 호출
     *
     * @param blogTotal       네이버 블로그 검색 결과 수 (화제성)
     * @param latestPostDate  최신 글 날짜 "yyyyMMdd" (Time-Decay)
     * @param descriptions    최신 5개 글 요약본 (감성 반응)
     */
    public void computeAiRating(int blogTotal, String latestPostDate, List<String> descriptions) {
        this.blogCount = blogTotal;

        // A. 기본 점수 — 생존 검증 통과 필수
        boolean notClosed = tags == null || !tags.contains("폐업");
        if (!Boolean.TRUE.equals(isVerified) || !notClosed) {
            this.aiRating = 0.0;
            this.blogPositiveTags = null;
            this.blogNegativeTags = null;
            return;
        }
        double score = 2.0;

        // B. 내부 정보 (최대 1.0점)
        boolean petFriendly = "Y".equalsIgnoreCase(chkPetInside)
                || (tags != null && tags.contains("대형견"));
        if (petFriendly) score += 0.4;
        if (imageUrl != null && !imageUrl.isBlank()
                && overview != null && overview.length() >= 50) score += 0.3;
        if (phone != null && !phone.isBlank()
                && homepage != null && !homepage.isBlank()) score += 0.3;

        // C. 화제성 (최대 1.0점, Time-Decay 적용)
        double blogScore = 0.0;
        if (blogTotal >= 100)     blogScore = 1.0;
        else if (blogTotal >= 50) blogScore = 0.7;
        else if (blogTotal >= 10) blogScore = 0.4;
        else if (blogTotal >= 1)  blogScore = 0.1;
        score += blogScore * computeDecayFactor(latestPostDate);

        // D. 감성 반응 (최대 1.0점) — 키워드 추출 후 저장
        List<String> foundPositive = extractPositiveKeywords(descriptions);
        List<String> foundNegative = extractNegativeKeywords(descriptions);
        int positiveCount = foundPositive.size();
        if (positiveCount >= 5)      score += 1.0;
        else if (positiveCount >= 2) score += 0.5;

        this.blogPositiveTags = foundPositive.isEmpty() ? null : String.join(",", foundPositive);
        this.blogNegativeTags = foundNegative.isEmpty() ? null : String.join(",", foundNegative);

        this.aiRating = Math.round(score * 10.0) / 10.0;
    }

    /** Time-Decay 계수 — 최신 글 날짜(yyyyMMdd) 기준 */
    private double computeDecayFactor(String latestPostDate) {
        if (latestPostDate == null || latestPostDate.isBlank()) return 0.1;
        try {
            LocalDate postDate = LocalDate.parse(latestPostDate, DateTimeFormatter.BASIC_ISO_DATE);
            long months = ChronoUnit.MONTHS.between(postDate, LocalDate.now());
            if (months <= 3)  return 1.0;
            if (months <= 6)  return 0.8;
            if (months <= 12) return 0.5;
            return 0.1;
        } catch (Exception e) {
            return 0.1;
        }
    }

    /** 긍정 키워드 추출 — 블로그 description에서 발견된 키워드 목록 반환 */
    private List<String> extractPositiveKeywords(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) return List.of();
        List<String> keywords = Arrays.asList(
                "추천", "강추", "맛있", "친절", "깨끗", "예쁘", "분위기",
                "재방문", "만족", "훌륭", "편안", "최고", "좋아", "좋았"
        );
        String combined = String.join(" ", descriptions).toLowerCase();
        return keywords.stream().filter(combined::contains).toList();
    }

    /** 부정 키워드 추출 — 블로그 description에서 발견된 부정 키워드 목록 반환 */
    private List<String> extractNegativeKeywords(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) return List.of();
        List<String> keywords = Arrays.asList(
                "불편", "시끄럽", "냄새", "별로", "실망",
                "협소", "지저분", "불친절", "후회", "비싸", "좁아"
        );
        String combined = String.join(" ", descriptions).toLowerCase();
        return keywords.stream().filter(combined::contains).toList();
    }

    /** 리뷰 추가 시 평점·리뷰수 갱신 */
    public void addReview(double newRating) {
        double total = this.rating * this.reviewCount + newRating;
        this.reviewCount++;
        this.rating = Math.round((total / this.reviewCount) * 10.0) / 10.0;
    }

    /** 리뷰 삭제 시 평점·리뷰수 갱신 */
    public void removeReview(double removedRating) {
        if (this.reviewCount <= 1) {
            this.reviewCount = 0;
            this.rating = 0.0;
        } else {
            double total = this.rating * this.reviewCount - removedRating;
            this.reviewCount--;
            this.rating = Math.round((total / this.reviewCount) * 10.0) / 10.0;
        }
    }

    /** 네이버 지역 검색 API 이미지 보강 */
    public void enrichImageFromNaver(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** 배치 Upsert — 공공데이터 + Kakao/Naver 교차검증 결과 반영 */
    public void upsertFromBatch(String title, String address, String addr2,
                                Double latitude, Double longitude,
                                Point geom, String category, String imageUrl, String phone,
                                String overview, String homepage,
                                String chkPetInside, String accomCountPet,
                                String petTurnAdroose, String operatingHours,
                                boolean isVerified, String kakaoId) {
        this.title = title;
        this.address = address;
        this.addr2 = addr2;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geom = geom;
        this.category = category;
        if (imageUrl != null) this.imageUrl = imageUrl;
        this.phone = phone;
        this.overview = overview;
        this.homepage = homepage;
        this.chkPetInside = chkPetInside;
        this.accomCountPet = accomCountPet;
        this.petTurnAdroose = petTurnAdroose;
        if (operatingHours != null) this.operatingHours = operatingHours;
        this.isVerified = isVerified;
        if (kakaoId != null) this.kakaoId = kakaoId;
    }
}
