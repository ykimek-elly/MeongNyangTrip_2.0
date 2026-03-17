package com.team.meongnyang.checkin.entity;

import com.team.meongnyang.common.BaseEntity;
import com.team.meongnyang.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 방문 인증(CheckIn) 엔티티.
 * 사용자가 장소를 방문하고 사진으로 인증한 기록을 저장한다.
 */
@Entity
@Table(name = "check_ins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CheckIn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id")
    private Long checkinId;

    /** 인증한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 장소명 (역지오코딩 결과) */
    @Column(nullable = false, length = 200)
    private String placeName;

    /** 위도 */
    @Column(nullable = false)
    private Double latitude;

    /** 경도 */
    @Column(nullable = false)
    private Double longitude;

    /** 사진 URL (S3 업로드 후 URL, 현재는 임시 처리) */
    @Column(name = "photo_url")
    private String photoUrl;

    /** 획득한 뱃지 이름 (예: "첫 방문", "연속 방문 7일") */
    @Column(name = "badge_name", length = 50)
    private String badgeName;
}
