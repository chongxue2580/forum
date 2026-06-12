package com.example.blog_froum.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 点赞服务接口
 */
public interface LikeService {

    /**
     * 点赞目标
     */
    void likeTarget(Long userId, String targetType, Long targetId);

    /**
     * 取消点赞目标
     */
    void unlikeTarget(Long userId, String targetType, Long targetId);

    /**
     * 检查用户是否已点赞目标
     */
    boolean isTargetLikedByUser(Long userId, String targetType, Long targetId);

    /**
     * 获取目标的点赞数
     */
    long getTargetLikeCount(String targetType, Long targetId);

    /**
     * 获取用户点赞的文章
     */
    Page<Long> getUserLikedArticles(Long userId, Pageable pageable);

    /**
     * 获取用户点赞的问答
     */
    Page<Long> getUserLikedQuestions(Long userId, Pageable pageable);

    /**
     * 获取用户点赞的评论
     */
    Page<Long> getUserLikedComments(Long userId, Pageable pageable);

    /**
     * 获取用户的总点赞数
     */
    long getUserTotalLikeCount(Long userId);
}
