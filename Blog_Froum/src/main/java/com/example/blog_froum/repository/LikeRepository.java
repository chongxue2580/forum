package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 点赞Repository
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * 查找用户对特定目标的点赞记录
     */
    Optional<Like> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    /**
     * 检查用户是否已点赞
     */
    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    /**
     * 删除用户对特定目标的点赞
     */
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    /**
     * 统计目标的点赞数
     */
    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 获取用户点赞的文章列表
     */
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.targetType = 'ARTICLE' ORDER BY l.createdAt DESC")
    Page<Like> findUserLikedArticles(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取用户点赞的问答列表
     */
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.targetType = 'QUESTION' ORDER BY l.createdAt DESC")
    Page<Like> findUserLikedQuestions(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取用户点赞的评论列表
     */
    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.targetType = 'COMMENT' ORDER BY l.createdAt DESC")
    Page<Like> findUserLikedComments(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取目标的点赞用户列表
     */
    List<Like> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    /**
     * 统计用户的总点赞数
     */
    long countByUserId(Long userId);
}
