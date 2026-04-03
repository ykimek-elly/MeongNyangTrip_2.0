package com.team.meongnyang.dm.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.dm.dto.DmDto;
import com.team.meongnyang.dm.service.DmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dms")
@RequiredArgsConstructor
public class DmController {

    private final DmService dmService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DmDto.ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<DmDto.ConversationResponse> response = dmService.getConversations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("대화 목록 조회 성공", response));
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<ApiResponse<List<DmDto.MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long partnerId) {
        List<DmDto.MessageResponse> response = dmService.getMessages(userDetails.getUsername(), partnerId);
        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }

    @PostMapping("/{partnerId}")
    public ResponseEntity<ApiResponse<DmDto.MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long partnerId,
            @RequestBody DmDto.SendRequest request) {
        DmDto.MessageResponse response = dmService.sendMessage(userDetails.getUsername(), partnerId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success("메시지 전송 성공", response));
    }

    @PatchMapping("/{partnerId}/read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long partnerId) {
        dmService.markAllAsRead(userDetails.getUsername(), partnerId);
        return ResponseEntity.ok(ApiResponse.success("메시지 읽음 처리 성공", null));
    }
}
