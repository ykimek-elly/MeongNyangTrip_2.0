package com.team.meongnyang.recommendation.batch;

import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 알림 발송 가능 여부를 판단하는 정책 서비스다.
 *
 * <p>사용자 상태, 연락처 보유 여부, 하루 1회 제한, 허용 시간대를 함께 검사한다.
 */
@Service
public class NotificationPolicyService {

    /**
     * 현재 시점에 알림 발송이 가능한지 판단한다.
     *
     * @param user 사용자
     * @param pet 대표 반려동물
     * @param now 현재 시각
     * @param alreadySentToday 오늘 발송 여부
     * @return 정책 판단 결과
     */
    public NotificationDecision evaluate(User user, Pet pet, LocalDateTime now, boolean alreadySentToday) {
        if (user == null || user.getStatus() != User.Status.ACTIVE || user.getRole() != User.Role.USER) {
            return NotificationDecision.skip(NotificationSkipReason.USER_NOT_ELIGIBLE);
        }
        if (!user.isNotificationEnabled()) {
            return NotificationDecision.skip(NotificationSkipReason.NOTIFICATION_DISABLED);
        }
        if (!StringUtils.hasText(user.getPhoneNumber())) {
            return NotificationDecision.skip(NotificationSkipReason.MISSING_PHONE_NUMBER);
        }
        if (pet == null) {
            return NotificationDecision.skip(NotificationSkipReason.PET_NOT_FOUND);
        }
        if (alreadySentToday) {
            return NotificationDecision.skip(NotificationSkipReason.ALREADY_SENT_TODAY);
        }
        return NotificationDecision.send();
    }

    /**
     * 알림 정책 판정 결과를 표현한다.
     */
    @Getter
    @Builder
    public static class NotificationDecision {
        private final boolean send;
        private final NotificationSkipReason reason;

        public static NotificationDecision send() {
            return NotificationDecision.builder()
                    .send(true)
                    .reason(null)
                    .build();
        }

        public static NotificationDecision skip(NotificationSkipReason reason) {
            return NotificationDecision.builder()
                    .send(false)
                    .reason(reason)
                    .build();
        }
    }

    /**
     * 알림을 건너뛴 이유를 나타낸다.
     */
    public enum NotificationSkipReason {
        USER_NOT_ELIGIBLE,
        NOTIFICATION_DISABLED,
        MISSING_PHONE_NUMBER,
        PET_NOT_FOUND,
        ALREADY_SENT_TODAY
    }
}
