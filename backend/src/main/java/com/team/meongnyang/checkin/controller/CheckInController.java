package com.team.meongnyang.checkin.controller;

import com.team.meongnyang.checkin.dto.CheckInDto;
import com.team.meongnyang.checkin.service.CheckInService;
import com.team.meongnyang.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 방문 인증 저장
     * POST /api/checkins
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckInDto.Response>> createCheckIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CheckInDto.Request request) {

        CheckInDto.Response response = checkInService.createCheckIn(
                userDetails.getUsername(), request);

        return ResponseEntity.ok(ApiResponse.success("방문 인증이 완료됐어요!", response));
    }

    /**
     * 내 방문 통계 + 기록 + 뱃지 조회
     * GET /api/checkins/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<CheckInDto.StatsResponse>> getMyStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        CheckInDto.StatsResponse response = checkInService.getMyStats(
                userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }
}
