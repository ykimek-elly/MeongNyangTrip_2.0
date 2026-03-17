package com.team.meongnyang.checkin.dto;

import com.team.meongnyang.checkin.entity.CheckIn;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class CheckInDto {

    /** 방문 인증 요청 */
    @Getter
    public static class Request {
        private String placeName;
        private Double latitude;
        private Double longitude;
        private String photoUrl; // 임시: 추후 S3 업로드로 교체
    }

    /** 방문 인증 응답 (단건) */
    @Getter
    public static class Response {
        private final Long checkinId;
        private final String placeName;
        private final Double latitude;
        private final Double longitude;
        private final String photoUrl;
        private final String badgeName;
        private final LocalDateTime checkedInAt;

        public Response(CheckIn c) {
            this.checkinId   = c.getCheckinId();
            this.placeName   = c.getPlaceName();
            this.latitude    = c.getLatitude();
            this.longitude   = c.getLongitude();
            this.photoUrl    = c.getPhotoUrl();
            this.badgeName   = c.getBadgeName();
            this.checkedInAt = c.getRegDate();
        }
    }

    /** 내 방문 통계 응답 */
    @Getter
    public static class StatsResponse {
        private final long totalVisits;
        private final long thisMonthVisits;
        private final int unlockedBadges;
        private final List<BadgeDto> badges;
        private final List<Response> recentHistory;

        public StatsResponse(long totalVisits, long thisMonthVisits,
                             List<BadgeDto> badges, List<Response> recentHistory) {
            this.totalVisits      = totalVisits;
            this.thisMonthVisits  = thisMonthVisits;
            this.unlockedBadges   = (int) badges.stream().filter(BadgeDto::isUnlocked).count();
            this.badges           = badges;
            this.recentHistory    = recentHistory;
        }
    }

    /** 뱃지 정보 */
    @Getter
    public static class BadgeDto {
        private final int id;
        private final String name;
        private final String icon;
        private final String description;
        private final boolean unlocked;

        public BadgeDto(int id, String name, String icon, String description, boolean unlocked) {
            this.id          = id;
            this.name        = name;
            this.icon        = icon;
            this.description = description;
            this.unlocked    = unlocked;
        }
    }
}
