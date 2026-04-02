package com.team.meongnyang.lounge.service;

import com.team.meongnyang.lounge.dto.LoungeDto;
import com.team.meongnyang.lounge.entity.LoungeComment;
import com.team.meongnyang.lounge.entity.LoungeLike;
import com.team.meongnyang.lounge.entity.LoungePost;
import com.team.meongnyang.lounge.repository.LoungeCommentRepository;
import com.team.meongnyang.lounge.repository.LoungeLikeRepository;
import com.team.meongnyang.lounge.repository.LoungePostRepository;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoungeService {

    private final LoungePostRepository postRepository;
    private final LoungeCommentRepository commentRepository;
    private final LoungeLikeRepository likeRepository;
    private final UserRepository userRepository;

    /** 피드 조회 (type: FEED or TALK) */
    public List<LoungeDto.PostResponse> getPosts(String currentUserEmail, String type) {
        String email = currentUserEmail != null ? currentUserEmail : "";
        List<LoungePost> posts;

        if ("TALK".equalsIgnoreCase(type)) {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            posts = postRepository.findByIsHiddenFalseAndPostTypeAndRegDateAfterOrderByPostIdDesc("TALK", since);
        } else {
            posts = postRepository.findByIsHiddenFalseAndPostTypeOrderByPostIdDesc("FEED");
        }

        return posts.stream()
                .map(p -> LoungeDto.PostResponse.from(p, email))
                .collect(Collectors.toList());
    }

    /** 게시글 작성 */
    @Transactional
    public LoungeDto.PostResponse createPost(String email, LoungeDto.CreateRequest req) {
        User user = getUser(email);
        String type = req.getPostType() != null ? req.getPostType().toUpperCase() : "FEED";
        LoungePost post = LoungePost.builder()
                .user(user)
                .content(req.getContent())
                .imageUrl(req.getImageUrl())
                .placeId(req.getPlaceId())
                .postType(type)
                .build();
        postRepository.save(post);
        return LoungeDto.PostResponse.from(post, email);
    }

    /** 게시글 수정 */
    @Transactional
    public LoungeDto.PostResponse updatePost(String email, Long postId, LoungeDto.UpdateRequest req) {
        LoungePost post = getPost(postId);
        if (!post.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 게시글만 수정할 수 있어요.");
        }
        post.updateContent(req.getContent());
        return LoungeDto.PostResponse.from(post, email);
    }

    /** 게시글 삭제 */
    @Transactional
    public void deletePost(String email, Long postId) {
        LoungePost post = getPost(postId);
        User user = getUser(email);
        if (!post.getUser().getEmail().equals(email) && !user.getRole().name().equals("ADMIN")) {
            throw new IllegalArgumentException("권한이 없어요.");
        }
        postRepository.delete(post);
    }

    /** 좋아요 토글 */
    @Transactional
    public LoungeDto.PostResponse toggleLike(String email, Long postId) {
        LoungePost post = getPost(postId);
        User user = getUser(email);

        likeRepository.findByPost_PostIdAndUser_Email(postId, email)
                .ifPresentOrElse(like -> {
                    likeRepository.delete(like);
                    post.decrementLikes();
                }, () -> {
                    likeRepository.save(LoungeLike.builder().post(post).user(user).build());
                    post.incrementLikes();
                });

        return LoungeDto.PostResponse.from(post, email);
    }

    /** 댓글 작성 */
    @Transactional
    public LoungeDto.CommentResponse addComment(String email, Long postId, LoungeDto.CommentRequest req) {
        LoungePost post = getPost(postId);
        User user = getUser(email);
        LoungeComment comment = LoungeComment.builder()
                .post(post)
                .user(user)
                .content(req.getContent())
                .build();
        commentRepository.save(comment);
        return LoungeDto.CommentResponse.from(comment, email);
    }

    /** 댓글 수정 */
    @Transactional
    public LoungeDto.CommentResponse updateComment(String email, Long postId, Long commentId, LoungeDto.CommentRequest req) {
        LoungeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없어요."));
        // postId 일치 검증 (다른 게시글 댓글 조작 방지)
        if (!comment.getPost().getPostId().equals(postId)) {
            throw new IllegalArgumentException("잘못된 요청이에요.");
        }
        if (!comment.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 댓글만 수정할 수 있어요.");
        }
        comment.updateContent(req.getContent());
        return LoungeDto.CommentResponse.from(comment, email);
    }

    /** 댓글 삭제 */
    @Transactional
    public void deleteComment(String email, Long postId, Long commentId) {
        LoungeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없어요."));
        if (!comment.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있어요.");
        }
        commentRepository.delete(comment);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요."));
    }

    private LoungePost getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없어요."));
    }
}