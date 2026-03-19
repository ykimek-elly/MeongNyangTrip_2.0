package com.team.meongnyang.user.entity;

import com.team.meongnyang.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원 엔티티 (ERD: USER)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** 소셜 로그인 사용자는 password = null */
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

    /** 소셜 로그인 제공자 (GOOGLE, KAKAO, null=이메일 가입) */
    @Column(length = 20)
    private String provider;

    /** 소셜 제공자의 고유 사용자 ID */
    @Column(name = "provider_id", length = 100)
    private String providerId;

    /** 프로필 이미지 URL (소셜 로그인 시 수집) */
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    /** 사용자 전화번호 (카카오 알림 발송용) */
    @Column(length = 20)
    private String phoneNumber;

    /** 알림 수신 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    /** 마지막 알림 발송 시각 */
    @Column
    private LocalDateTime lastNotificationSentAt;

    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        ACTIVE, SUSPENDED, BLOCK, DELETED
    }

    /** 닉네임 업데이트 (소셜 로그인 재방문 시 최신화) */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 비밀번호 업데이트 (BCrypt 인코딩된 값으로 교체) */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 회원 탈퇴 — DELETED 상태로 변경 (소프트 딜리트) */
    public void markAsDeleted() {
        this.status = Status.DELETED;
    }
}
