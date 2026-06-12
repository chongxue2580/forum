package com.example.blog_froum.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", insertable = false, updatable = false)
    private User fromUser;

    @Column(nullable = false, length = 50)
    private String type; // ARTICLE_COMMENT, QUESTION_COMMENT, USER_FOLLOW, QUESTION_FOLLOW, ARTICLE_LIKE, etc.

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 构造函数
     */
    public Notification(Long userId, Long fromUserId, String type, String title, String content, Long targetId) {
        this.userId = userId;
        this.fromUserId = fromUserId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetId = targetId;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
