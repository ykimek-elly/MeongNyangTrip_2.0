package com.team.meongnyang.friend.service;

import com.team.meongnyang.friend.dto.FriendDto;
import com.team.meongnyang.friend.dto.ShareRequest;
import com.team.meongnyang.friend.entity.Friend;
import com.team.meongnyang.friend.entity.ShareRecord;
import com.team.meongnyang.friend.repository.FriendRepository;
import com.team.meongnyang.friend.repository.ShareRecordRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final ShareRecordRepository shareRecordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FriendDto> getFriends(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return friendRepository.findByUser(user).stream()
                .map(friend -> convertToDto(friend.getFriendUser(), true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getSuggestedFriends(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(u -> !u.getUserId().equals(user.getUserId()))
                .filter(u -> !friendRepository.existsByUserAndFriendUser(user, u))
                .limit(10) // 임의로 10명만 추천
                .map(u -> convertToDto(u, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addFriend(String email, Long friendUserId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        User friendUser = userRepository.findById(friendUserId).orElseThrow(() -> new RuntimeException("Friend user not found"));
        
        if (!friendRepository.existsByUserAndFriendUser(user, friendUser)) {
            friendRepository.save(Friend.builder()
                    .user(user)
                    .friendUser(friendUser)
                    .build());
        }
    }

    @Transactional
    public void sharePost(String email, ShareRequest request) {
        User sender = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        
        for (String friendIdStr : request.getFriendIds()) {
            Long friendId = Long.parseLong(friendIdStr);
            User receiver = userRepository.findById(friendId).orElseThrow(() -> new RuntimeException("Receiver not found"));
            
            shareRecordRepository.save(ShareRecord.builder()
                    .postId(request.getPostId())
                    .sender(sender)
                    .receiver(receiver)
                    .message(request.getMessage())
                    .build());
        }
    }

    private FriendDto convertToDto(User user, boolean isVerified) {
        String petName = "반려동물";
        String petType = "강아지";
        
        if (user.getPets() != null && !user.getPets().isEmpty()) {
            Pet firstPet = user.getPets().get(0);
            petName = firstPet.getPetName();
            petType = firstPet.getPetType() != null ? firstPet.getPetType().name() : "강아지";
        }
        
        return FriendDto.builder()
                .id(String.valueOf(user.getUserId()))
                .name(user.getNickname() != null ? user.getNickname() : "알 수 없음")
                .profileImg(user.getProfileImage() != null ? user.getProfileImage() : "https://via.placeholder.com/150")
                .petName(petName)
                .petType(petType)
                .isVerified(isVerified)
                .build();
    }
}
