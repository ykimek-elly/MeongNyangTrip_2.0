package com.team.meongnyang.friend.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return friendRepository.findByUser(user).stream()
                .map(friend -> convertToDto(friend.getFriendUser(), true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getSuggestedFriends(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        @SuppressWarnings("null")
        User friendUser = userRepository.findById(friendUserId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Friend user not found"));
        
        if (!friendRepository.existsByUserAndFriendUser(user, friendUser)) {
            @SuppressWarnings("null")
            Friend saved = Friend.builder()
                    .user(user)
                    .friendUser(friendUser)
                    .build();
            friendRepository.save(saved);
        }
    }

    @Transactional
    public void sharePost(String email, ShareRequest request) {
        User sender = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        for (String friendIdStr : request.getFriendIds()) {
            Long friendId = Long.parseLong(friendIdStr);
            
            @SuppressWarnings("null")
            User receiver = userRepository.findById(friendId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Receiver not found"));
            
            @SuppressWarnings("null")
            ShareRecord record = ShareRecord.builder()
                    .postId(request.getPostId())
                    .sender(sender)
                    .receiver(receiver)
                    .message(request.getMessage())
                    .build();
            shareRecordRepository.save(record);
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
