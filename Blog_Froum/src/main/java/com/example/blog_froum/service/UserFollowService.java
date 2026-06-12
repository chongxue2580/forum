package com.example.blog_froum.service;

import com.example.blog_froum.dto.user.UserResponse;

import java.util.List;

/**
 * 用户关注服务接口
 */
public interface UserFollowService {

    /**
     * 关注用户
     */
    void followUser(Long userId, Long targetUserId);

    /**
     * 取消关注
     */
    void unfollowUser(Long userId, Long targetUserId);

    /**
     * 获取用户的关注者列表（粉丝）
     */
    List<UserResponse> getFollowers(Long userId, int page, int size);

    /**
     * 获取用户关注的用户列表
     */
    List<UserResponse> getFollowing(Long userId, int page, int size);

    /**
     * 检查是否关注
     */
    boolean isFollowing(Long userId, Long targetUserId);

    /**
     * 获取用户的关注数量
     */
    long countFollowing(Long userId);

    /**
     * 获取用户的粉丝数量
     */
    long countFollowers(Long userId);
} 