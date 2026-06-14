package com.example.blog_froum.dto.comment;

import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 评论响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String targetType; // 目标类型：ARTICLE, QUESTION
    private Long targetId; // 目标ID
    private Long articleId; // 保留用于向后兼容
    private UserResponse user;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Boolean isBestAnswer;
    private Boolean isDeleted; // 是否已删除（管理端展示与恢复用）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies; // 子评论列表

    public static CommentResponse fromEntity(Comment comment) {
        Objects.requireNonNull(comment, "comment");

        return CommentResponse.builder()
                .id(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .articleId("ARTICLE".equals(comment.getTargetType()) ? comment.getTargetId() : null) // 向后兼容
                .user(comment.getUser() != null ? UserResponse.fromUser(comment.getUser()) : null)
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .isBestAnswer(Boolean.TRUE.equals(comment.getIsBestAnswer()))
                .isDeleted(Boolean.TRUE.equals(comment.getIsDeleted()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
