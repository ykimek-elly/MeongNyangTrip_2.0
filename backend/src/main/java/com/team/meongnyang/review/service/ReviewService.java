package com.team.meongnyang.review.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.review.dto.ReviewDto;
import com.team.meongnyang.review.entity.Review;
import com.team.meongnyang.review.repository.ReviewRepository;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    /**
     * 리뷰 작성 — 장소당 1인 1리뷰
     */
    @Transactional
    public ReviewDto.Response createReview(String email, Long placeId, ReviewDto.Request request) {
        User user = findUser(email);
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        if (reviewRepository.existsByUser_UserIdAndPlace_Id(user.getUserId(), placeId)) {
            throw new RuntimeException("이미 리뷰를 작성하셨습니다.");
        }

        Review review = Review.builder()
                .user(user)
                .place(place)
                .content(request.getContent())
                .rating(request.getRating())
                .imageUrl(request.getImageUrl())
                .build();

        reviewRepository.save(review);
        place.addReview(request.getRating());

        return new ReviewDto.Response(review);
    }

    /**
     * 장소 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public ReviewDto.PlaceReviewsResponse getReviewsByPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        List<ReviewDto.Response> reviews = reviewRepository
                .findByPlace_IdOrderByRegDateDesc(placeId)
                .stream()
                .map(ReviewDto.Response::new)
                .toList();

        return new ReviewDto.PlaceReviewsResponse(place.getRating(), place.getReviewCount(), reviews);
    }

    /**
     * 내 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewDto.Response> getMyReviews(String email) {
        User user = findUser(email);
        return reviewRepository.findByUser_UserIdOrderByRegDateDesc(user.getUserId())
                .stream()
                .map(ReviewDto.Response::new)
                .toList();
    }

    /**
     * 리뷰 삭제 — 본인 리뷰만 삭제 가능
     */
    @Transactional
    public void deleteReview(String email, Long reviewId) {
        User user = findUser(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("본인 리뷰만 삭제할 수 있습니다.");
        }

        review.getPlace().removeReview(review.getRating());
        reviewRepository.delete(review);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
