package com.example.blog_froum.service;

import com.example.blog_froum.dto.comment.CommentCreateRequest;
import com.example.blog_froum.dto.comment.CommentResponse;
import com.example.blog_froum.dto.statistics.CommentStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 创建评论
     */
    CommentResponse createComment(Long userId, CommentCreateRequest request);
    
    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 获取单条评论详情
     */
    CommentResponse getCommentById(Long commentId);
    
    /**
     * 获取文章的评论列表（包含子评论）
     */
    Page<CommentResponse> getArticleComments(Long articleId, Pageable pageable);
    
    /**
     * 获取用户的评论列表
     */
    Page<CommentResponse> getUserComments(Long userId, Pageable pageable);
    
    /**
     * 点赞评论
     */
    void likeComment(Long commentId, Long userId);
    
    /**
     * 取消点赞评论
     */
    void unlikeComment(Long commentId, Long userId);

    /**
     * 获取问答的评论列表
     */
    Page<CommentResponse> getQuestionComments(Long questionId, Pageable pageable);

    /**
     * 获取目标的评论列表
     */
    Page<CommentResponse> getTargetComments(String targetType, Long targetId, Pageable pageable);

    // 管理员功能
    /**
     * 获取所有评论（管理员）
     */
    Page<CommentResponse> getAllComments(Pageable pageable);

    /**
     * 根据目标类型获取评论
     */
    Page<CommentResponse> getCommentsByTargetType(String targetType, Pageable pageable);

    /**
     * 根据目标类型和目标ID获取评论
     */
    Page<CommentResponse> getCommentsByTarget(String targetType, Long targetId, Pageable pageable);

    /**
     * 管理员删除评论
     */
    void adminDeleteComment(Long commentId);

    /**
     * 管理员恢复（撤销软删除）评论
     */
    void adminRestoreComment(Long commentId);

    /**
     * 批量删除评论
     */
    void batchDeleteComments(java.util.List<Long> commentIds);

    /**
     * 获取评论统计信息
     */
    CommentStatisticsResponse getCommentStatistics();

    /**
     * 获取最近的评论
     */
    Page<CommentResponse> getRecentComments(Pageable pageable);

    /**
     * 管理员筛选评论。
     * @param deleted 删除状态过滤：null=全部，false=未删除，true=已删除
     */
    Page<CommentResponse> searchCommentsForAdmin(String targetType,
                                                 Long targetId,
                                                 String keyword,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 Boolean deleted,
                                                 Pageable pageable);
}
