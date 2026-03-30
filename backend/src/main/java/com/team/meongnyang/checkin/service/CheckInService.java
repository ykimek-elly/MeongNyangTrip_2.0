package com.team.meongnyang.checkin.service;

import com.team.meongnyang.checkin.dto.CheckInDto;
import com.team.meongnyang.checkin.entity.CheckIn;
import com.team.meongnyang.checkin.repository.CheckInRepository;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;

    // 뱃지 정의
    private static final List<CheckInDto.BadgeDto> ALL_BADGES = List.of(
        new CheckInDto.BadgeDto(1, "첫 방문",      "🎉", "첫 장소 방문 완료",    false),
        new CheckInDto.BadgeDto(2, "연속 방문 7일", "🔥", "7일 연속 체크인",      false),
        new CheckInDto.BadgeDto(3, "장소 탐험가",   "🗺️", "10곳 이상 방문",       false),
        new CheckInDto.BadgeDto(4, "사진 수집가",   "📸", "30장 이상 업로드",     false),
        new CheckInDto.BadgeDto(5, "리뷰 마스터",   "✍️", "20개 리뷰 작성",       false),
        new CheckInDto.BadgeDto(6, "인기스타",      "⭐", "좋아요 100개 받기",    false)
    );

    /**
     * 방문 인증 저장
     */
    @Transactional
    public CheckInDto.Response createCheckIn(String email, CheckInDto.Request request) {
        User user = findUser(email);

        long totalVisits = checkInRepository.countByUser_UserId(user.getUserId());
        String badgeName = decideBadge(totalVisits + 1);

        CheckIn checkIn = CheckIn.builder()
                .user(user)
                .placeName(request.getPlaceName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .photoUrl(request.getPhotoUrl())
                .badgeName(badgeName)
                .build();

        return new CheckInDto.Response(checkInRepository.save(checkIn));
    }

    /**
     * 내 방문 통계 + 기록 조회
     */
    @Transactional(readOnly = true)
    public CheckInDto.StatsResponse getMyStats(String email) {
        User user = findUser(email);
        Long userId = user.getUserId();

        long totalVisits     = checkInRepository.countByUser_UserId(userId);
        long thisMonthVisits = checkInRepository.countThisMonth(userId,
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));

        List<CheckInDto.Response> recentHistory = checkInRepository
                .findByUser_UserIdOrderByRegDateDesc(userId)
                .stream()
                .limit(10)
                .map(CheckInDto.Response::new)
                .toList();

        List<CheckInDto.BadgeDto> badges = ALL_BADGES.stream()
                .map(b -> new CheckInDto.BadgeDto(
                        b.getId(), b.getName(), b.getIcon(), b.getDescription(),
                        isBadgeUnlocked(b.getId(), totalVisits)))
                .toList();

        return new CheckInDto.StatsResponse(totalVisits, thisMonthVisits, badges, recentHistory);
    }

    // ── private ──────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }

    /** 방문 횟수에 따라 뱃지 결정 */
    private String decideBadge(long visitCount) {
        if (visitCount == 1)  return "첫 방문";
        if (visitCount == 10) return "장소 탐험가";
        return null;
    }

    /** 뱃지 해금 조건 */
    private boolean isBadgeUnlocked(int badgeId, long totalVisits) {
        return switch (badgeId) {
            case 1 -> totalVisits >= 1;   // 첫 방문
            case 2 -> false;              // 연속 방문 7일 - 연속 방문 데이터 미구현
            case 3 -> totalVisits >= 10;  // 장소 탐험가
            case 4 -> false;              // 사진 수집가 - 추후 photoCount 연동
            case 5 -> false;              // 리뷰 마스터 - 추후 reviewCount 연동
            case 6 -> false;              // 인기스타 - 추후 likeCount 연동
            default -> false;
        };
    }
}
