package com.team.meongnyang.friend.controller;

import com.team.meongnyang.friend.dto.FriendDto;
import com.team.meongnyang.friend.dto.ShareRequest;
import com.team.meongnyang.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendService.getFriends(userDetails.getUsername()));
    }

    @GetMapping("/suggested")
    public ResponseEntity<List<FriendDto>> getSuggestedFriends(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendService.getSuggestedFriends(userDetails.getUsername()));
    }

    @PostMapping("/{friendUserId}")
    public ResponseEntity<Void> addFriend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long friendUserId) {
        friendService.addFriend(userDetails.getUsername(), friendUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/share")
    public ResponseEntity<Void> sharePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ShareRequest request) {
        friendService.sharePost(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}
