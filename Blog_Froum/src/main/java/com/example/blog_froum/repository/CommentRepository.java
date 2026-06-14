package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据访问层
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * 根据目标类型和目标ID查询评论（只查询顶级评论）
     */
    Page<Comment> findByTargetTypeAndTargetIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            String targetType, Long targetId, Pageable pageable);

    /**
     * 根据父评论ID查询子评论
     */
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);

    /**
     * 根据目标类型和目标ID统计评论数量
     */
    long countByTargetTypeAndTargetIdAndIsDeletedFalse(String targetType, Long targetId);
    
    /**
     * 根据用户ID查询评论
     */
    Page<Comment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据目标类型和目标ID查询评论（包含子评论）
     */
    Page<Comment> findByTargetTypeAndTargetIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String targetType, Long targetId, Pageable pageable);

    /**
     * 根据目标类型和目标ID分页查询评论（兼容旧版本）
     */
    Page<Comment> findByTargetTypeAndTargetIdAndIsDeletedFalseOrderByCreatedAtAsc(
            String targetType, Long targetId, Pageable pageable);

    /**
     * 查询指定问答下已标记为最佳答案的评论。
     */
    List<Comment> findByTargetTypeAndTargetIdAndIsBestAnswerTrueAndIsDeletedFalse(
            String targetType, Long targetId);

    // 管理员查询方法
    /**
     * 获取所有未删除的评论
     */
    Page<Comment> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据目标类型获取评论
     */
    Page<Comment> findByTargetTypeAndIsDeletedFalseOrderByCreatedAtDesc(String targetType, Pageable pageable);

    /**
     * 统计未删除的评论数量
     */
    long countByIsDeletedFalse();

    /**
     * 根据目标类型统计评论数量
     */
    long countByTargetTypeAndIsDeletedFalse(String targetType);

    /**
     * 统计用户发布的评论数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户发布的未删除评论数量
     */
    long countByUserIdAndIsDeletedFalse(Long userId);

    long countByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime start, LocalDateTime end);

    List<Comment> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    @Query(value = "SELECT c FROM Comment c LEFT JOIN FETCH c.user u " +
            "WHERE (:deleted IS NULL OR c.isDeleted = :deleted) " +
            "AND (:targetType IS NULL OR c.targetType = :targetType) " +
            "AND (:targetId IS NULL OR c.targetId = :targetId) " +
            "AND (:startTime IS NULL OR c.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR c.createdAt < :endTime) " +
            "AND (:keyword IS NULL OR " +
            "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY c.createdAt DESC",
            countQuery = "SELECT COUNT(c) FROM Comment c LEFT JOIN c.user u " +
                    "WHERE (:deleted IS NULL OR c.isDeleted = :deleted) " +
                    "AND (:targetType IS NULL OR c.targetType = :targetType) " +
                    "AND (:targetId IS NULL OR c.targetId = :targetId) " +
                    "AND (:startTime IS NULL OR c.createdAt >= :startTime) " +
                    "AND (:endTime IS NULL OR c.createdAt < :endTime) " +
                    "AND (:keyword IS NULL OR " +
                    "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Comment> searchForAdmin(@Param("targetType") String targetType,
                                 @Param("targetId") Long targetId,
                                 @Param("keyword") String keyword,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 @Param("deleted") Boolean deleted,
                                 Pageable pageable);
}
