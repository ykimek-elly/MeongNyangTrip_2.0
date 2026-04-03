package com.team.meongnyang.user.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 회원 엔티티다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** 소셜 로그인 사용자는 password가 null일 수 있다. */
    @Column(length = 100)
    private String password;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(unique = true, nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /** 로그인 제공자 정보다. */
    @Column(length = 20)
    private String provider;

    /** 제공자별 고유 사용자 ID다. */
    @Column(name = "provider_id", length = 100)
    private String providerId;

    /** 프로필 이미지 URL이다. */
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    /** 알림 발송에 사용하는 전화번호다. */
    @Column(length = 20)
    private String phoneNumber;

    /** 알림 수신 동의 여부다. */
    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    /** 마지막 알림 발송 시각이다. */
    @Column
    private LocalDateTime lastNotificationSentAt;

    /** 현재 위치 위도 (미설정 시 null) */
    @Column
    private Double latitude;

    /** 현재 위치 경도 (미설정 시 null) */
    @Column
    private Double longitude;

    /** 활동 반경 km (5 / 15 / 30, 기본값 15) */
    @Column(name = "activity_radius")
    @Builder.Default
    private Integer activityRadius = 15;

    /** 활동 지역 텍스트 (예: "서울 강남구") — 알림 발송 및 UI 표시용 */
    @Column(name = "region", length = 50)
    private String region;

    /** 반려동물 목록 */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pet> pets = new ArrayList<>();

    /**
     * 현재 시각 기준으로 알림 발송 시각을 갱신한다.
     */
    public void markNotificationSent() {
        this.lastNotificationSentAt = LocalDateTime.now(SEOUL_ZONE);
    }

    /**
     * 지정한 시각으로 알림 발송 시각을 갱신한다.
     *
     * @param sentAt 발송 시각
     */
    public void markNotificationSent(LocalDateTime sentAt) {
        this.lastNotificationSentAt = sentAt;
    }

    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        ACTIVE, SUSPENDED, BLOCK, DELETED
    }

    /**
     * 닉네임을 변경한다.
     *
     * @param nickname 새 닉네임
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 인코딩된 비밀번호로 교체한다.
     *
     * @param encodedPassword 인코딩된 비밀번호
     */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 회원 상태를 삭제로 변경한다.
     */
    public void markAsDeleted() {
        this.status = Status.DELETED;
    }

    /**
     * 탈퇴 회원이 소셜 재로그인 시 계정을 재활성화한다.
     */
    public void reactivate() {
        this.status = Status.ACTIVE;
    }

    /** 활동 지역 좌표 + 반경 + 지역명 저장 */
    public void updateLocation(Double latitude, Double longitude, Integer activityRadius, String region) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (activityRadius != null) this.activityRadius = activityRadius;
        if (region != null && !region.isBlank()) this.region = region;
    }

    /** 휴대폰 번호를 업데이트한다. */
    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /** 알림 설정을 업데이트한다. */
    public void updateNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
    }
}
