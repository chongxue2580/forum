package com.example.blog_froum.service;

import com.example.blog_froum.dto.user.UserStatsResponse;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.FollowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户统计服务
 */
@Service
@Slf4j
public class UserStatsService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FollowRepository followRepository;

    /**
     * 获取用户统计数据
     * @param userId 用户ID
     * @return 用户统计响应
     */
    public UserStatsResponse getUserStats(Long userId) {
        log.info("开始获取用户统计数据，用户ID: {}", userId);

        long articleCount = articleRepository.countByAuthorId(userId);
        long questionCount = questionRepository.countByAuthorId(userId);
        long commentCount = commentRepository.countByUserIdAndIsDeletedFalse(userId);
        long followersCount = followRepository.countByTargetIdAndTargetType(userId, "USER");
        long followingCount = followRepository.countByFollowerIdAndTargetType(userId, "USER");

        UserStatsResponse stats = new UserStatsResponse(
                articleCount,
                questionCount,
                commentCount,
                followersCount,
                followingCount);
        log.info("用户统计数据获取成功，用户ID: {}, 统计结果: {}", userId, stats);
        return stats;
    }
    
    /**
     * 获取用户详细统计信息（包含更多统计项）
     */
    public UserStatsResponse getDetailedUserStats(Long userId) {
        UserStatsResponse stats = getUserStats(userId);
        log.info("用户详细统计数据获取成功，用户ID: {}", userId);
        return stats;
    }
}
