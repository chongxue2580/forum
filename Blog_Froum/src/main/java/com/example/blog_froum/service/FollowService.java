package com.example.blog_froum.service;

import com.example.blog_froum.dto.follow.FollowQuestionResponse;
import com.example.blog_froum.dto.follow.FollowUserResponse;
import org.springframework.data.domain.Page;

/**
 * 关注服务接口
 */
public interface FollowService {

    /**
     * 关注目标
     */
    void followTarget(Long followerId, String targetType, Long targetId);

    /**
     * 取消关注目标
     */
    void unfollowTarget(Long followerId, String targetType, Long targetId);

    /**
     * 检查用户是否已关注目标
     */
    boolean isTargetFollowedByUser(Long followerId, String targetType, Long targetId);

    /**
     * 获取目标的关注数
     */
    long getTargetFollowCount(String targetType, Long targetId);





    /**
     * 获取用户关注的数量
     */
    long getUserFollowingCount(Long followerId, String targetType);

    /**
     * 获取用户的粉丝数量
     */
    long getUserFollowerCount(Long targetId);

    /**
     * 获取用户关注的用户列表（包含用户详细信息）
     */
    Page<FollowUserResponse> getUserFollowedUsers(Long followerId, int page, int size);

    /**
     * 获取用户的粉丝列表（包含用户详细信息）
     */
    Page<FollowUserResponse> getUserFollowers(Long targetId, int page, int size);

    /**
     * 获取用户关注的问题列表（包含问题详细信息）
     */
    Page<FollowQuestionResponse> getUserFollowedQuestions(Long followerId, int page, int size);
}
