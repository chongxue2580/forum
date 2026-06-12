package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 关注Repository
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    /**
     * 查找用户对特定目标的关注记录
     */
    Optional<Follow> findByFollowerIdAndTargetTypeAndTargetId(Long followerId, String targetType, Long targetId);

    /**
     * 检查用户是否已关注
     */
    boolean existsByFollowerIdAndTargetTypeAndTargetId(Long followerId, String targetType, Long targetId);

    /**
     * 删除用户对特定目标的关注
     */
    void deleteByFollowerIdAndTargetTypeAndTargetId(Long followerId, String targetType, Long targetId);

    /**
     * 统计目标的关注数
     */
    long countByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 获取用户关注的用户列表
     */
    @Query("SELECT f FROM Follow f WHERE f.followerId = :followerId AND f.targetType = 'USER' ORDER BY f.createdAt DESC")
    Page<Follow> findUserFollowedUsers(@Param("followerId") Long followerId, Pageable pageable);

    /**
     * 获取用户的粉丝列表
     */
    @Query("SELECT f FROM Follow f WHERE f.targetId = :targetId AND f.targetType = 'USER' ORDER BY f.createdAt DESC")
    Page<Follow> findUserFollowers(@Param("targetId") Long targetId, Pageable pageable);

    /**
     * 获取用户关注的问答列表
     */
    @Query("SELECT f FROM Follow f WHERE f.followerId = :followerId AND f.targetType = 'QUESTION' ORDER BY f.createdAt DESC")
    Page<Follow> findUserFollowedQuestions(@Param("followerId") Long followerId, Pageable pageable);

    /**
     * 获取问答的关注者列表
     */
    List<Follow> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    /**
     * 统计用户关注的数量
     */
    long countByFollowerIdAndTargetType(Long followerId, String targetType);

    /**
     * 统计用户的粉丝数量
     */
    long countByTargetIdAndTargetType(Long targetId, String targetType);
}
