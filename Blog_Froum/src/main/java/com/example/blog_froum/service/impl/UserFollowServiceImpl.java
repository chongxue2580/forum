package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.entity.UserFollow;
import com.example.blog_froum.mapper.UserFollowMapper;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.service.UserFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户关注服务实现类
 */
@Slf4j
@Service
@Transactional
public class UserFollowServiceImpl implements UserFollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void followUser(Long userId, Long targetUserId) {
        log.info("用户关注请求，用户ID: {}，目标用户ID: {}", userId, targetUserId);

        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            log.warn("用户关注失败，用户不存在，用户ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.findById(targetUserId);
        if (targetUser == null) {
            log.warn("用户关注失败，目标用户不存在，目标用户ID: {}", targetUserId);
            throw new RuntimeException("目标用户不存在");
        }

        // 不能关注自己
        if (userId.equals(targetUserId)) {
            log.warn("用户关注失败，不能关注自己，用户ID: {}", userId);
            throw new RuntimeException("不能关注自己");
        }

        // 检查是否已经关注
        if (userFollowMapper.exists(userId, targetUserId)) {
            log.warn("用户关注失败，已经关注过该用户，用户ID: {}，目标用户ID: {}", userId, targetUserId);
            throw new RuntimeException("已经关注过该用户");
        }

        // 创建关注关系
        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(userId);
        userFollow.setFollowingId(targetUserId);
        LocalDateTime now = LocalDateTime.now();
        userFollow.setCreatedAt(now);
        userFollow.setUpdatedAt(now);

        int result = userFollowMapper.insert(userFollow);
        if (result <= 0) {
            log.error("用户关注失败，数据库插入失败，用户ID: {}，目标用户ID: {}", userId, targetUserId);
            throw new RuntimeException("关注失败");
        }

        log.info("用户关注成功，用户ID: {}，目标用户ID: {}", userId, targetUserId);
    }

    @Override
    public void unfollowUser(Long userId, Long targetUserId) {
        log.info("取消关注请求，用户ID: {}，目标用户ID: {}", userId, targetUserId);

        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            log.warn("取消关注失败，用户不存在，用户ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.findById(targetUserId);
        if (targetUser == null) {
            log.warn("取消关注失败，目标用户不存在，目标用户ID: {}", targetUserId);
            throw new RuntimeException("目标用户不存在");
        }

        // 检查是否已经关注
        if (!userFollowMapper.exists(userId, targetUserId)) {
            log.warn("取消关注失败，未关注该用户，用户ID: {}，目标用户ID: {}", userId, targetUserId);
            throw new RuntimeException("未关注该用户");
        }

        int result = userFollowMapper.delete(userId, targetUserId);
        if (result <= 0) {
            log.error("取消关注失败，数据库删除失败，用户ID: {}，目标用户ID: {}", userId, targetUserId);
            throw new RuntimeException("取消关注失败");
        }

        log.info("取消关注成功，用户ID: {}，目标用户ID: {}", userId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getFollowers(Long userId, int page, int size) {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int offset = page * size;
        List<User> followers = userFollowMapper.findFollowers(userId, offset, size);
        return followers.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getFollowing(Long userId, int page, int size) {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int offset = page * size;
        List<User> following = userFollowMapper.findFollowing(userId, offset, size);
        return following.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long userId, Long targetUserId) {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.findById(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("目标用户不存在");
        }

        return userFollowMapper.exists(userId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return userFollowMapper.countFollowing(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return userFollowMapper.countFollowers(userId);
    }
} 