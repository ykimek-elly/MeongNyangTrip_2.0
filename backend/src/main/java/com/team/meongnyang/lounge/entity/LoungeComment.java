package com.team.meongnyang.lounge.entity;

import com.team.meongnyang.common.BaseEntity;
import com.team.meongnyang.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lounge_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoungeComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private LoungePost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public void updateContent(String content) {
        this.content = content;
    }
}
