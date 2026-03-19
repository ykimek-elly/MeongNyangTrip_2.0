package com.team.meongnyang.wishlist.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import com.team.meongnyang.wishlist.dto.WishlistDto;
import com.team.meongnyang.wishlist.entity.Wishlist;
import com.team.meongnyang.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    /**
     * 찜하기 토글 — 이미 찜한 경우 취소, 아니면 추가
     */
    @Transactional
    public WishlistDto.ToggleResponse toggle(String email, Long placeId) {
        User user = findUser(email);
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        boolean already = wishlistRepository.existsByUser_UserIdAndPlace_Id(user.getUserId(), placeId);
        if (already) {
            wishlistRepository.deleteByUser_UserIdAndPlace_Id(user.getUserId(), placeId);
            return new WishlistDto.ToggleResponse(false, placeId);
        } else {
            wishlistRepository.save(Wishlist.builder().user(user).place(place).build());
            return new WishlistDto.ToggleResponse(true, placeId);
        }
    }

    /**
     * 내 찜 목록 조회
     */
    @Transactional(readOnly = true)
    public List<WishlistDto.Response> getMyWishlists(String email) {
        User user = findUser(email);
        return wishlistRepository.findByUser_UserIdOrderByRegDateDesc(user.getUserId())
                .stream()
                .map(WishlistDto.Response::new)
                .toList();
    }

    /**
     * 특정 장소 찜 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isWishlisted(String email, Long placeId) {
        User user = findUser(email);
        return wishlistRepository.existsByUser_UserIdAndPlace_Id(user.getUserId(), placeId);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
