package com.team.meongnyang.dm.service;

import com.team.meongnyang.dm.dto.DmDto;
import com.team.meongnyang.dm.entity.DmMessage;
import com.team.meongnyang.dm.repository.DmMessageRepository;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DmService {

    private final DmMessageRepository dmMessageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DmDto.ConversationResponse> getConversations(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<DmMessage> latestMessages = dmMessageRepository.findLatestMessagesForUser(user.getUserId());

        return latestMessages.stream().map(msg -> {
            User partner = msg.getSender().equals(user) ? msg.getReceiver() : msg.getSender();
            long unread = dmMessageRepository.countUnreadMessages(user, partner);

            String partnerImg = ""; // Placeholder for profile image
            
            return DmDto.ConversationResponse.builder()
                    .partnerId(partner.getUserId().toString())
                    .partnerNickname(partner.getNickname())
                    .partnerImg(partnerImg)
                    .lastMessage(msg.getContent())
                    .lastMessageAt(msg.getRegDate())
                    .unreadCount(unread)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public List<DmDto.MessageResponse> getMessages(String username, Long partnerId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("상대방을 찾을 수 없습니다."));

        List<DmMessage> messages = dmMessageRepository.findConversation(user, partner);
        
        messages.stream()
                .filter(m -> m.getReceiver().equals(user) && !m.isRead())
                .forEach(DmMessage::markAsRead);

        return messages.stream()
                .map(msg -> DmDto.MessageResponse.builder()
                        .id(msg.getId())
                        .fromId(msg.getSender().getUserId().toString())
                        .content(msg.getContent())
                        .createdAt(msg.getRegDate())
                        .isRead(msg.isRead())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public DmDto.MessageResponse sendMessage(String username, Long partnerId, String content) {
        User sender = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("상대방을 찾을 수 없습니다."));

        DmMessage message = DmMessage.builder()
                .sender(sender)
                .receiver(partner)
                .content(content)
                .isRead(false)
                .build();

        dmMessageRepository.save(message);
        
        // flush to create createdAt if it's dependent on BaseEntity auditing
        dmMessageRepository.flush();

        return DmDto.MessageResponse.builder()
                .id(message.getId())
                .fromId(sender.getUserId().toString())
                .content(message.getContent())
                .createdAt(message.getRegDate())
                .isRead(message.isRead())
                .build();
    }

    @Transactional
    public void markAllAsRead(String username, Long partnerId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("상대방을 찾을 수 없습니다."));

        List<DmMessage> messages = dmMessageRepository.findConversation(user, partner);
        messages.stream()
                .filter(m -> m.getReceiver().equals(user) && !m.isRead())
                .forEach(DmMessage::markAsRead);
    }
}
