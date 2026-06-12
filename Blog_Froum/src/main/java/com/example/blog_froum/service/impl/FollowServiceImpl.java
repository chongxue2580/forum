package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.follow.FollowQuestionResponse;
import com.example.blog_froum.dto.follow.FollowUserResponse;
import com.example.blog_froum.entity.Follow;
import com.example.blog_froum.entity.Question;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.FollowRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 关注服务实现类
 */
@Slf4j
@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void followTarget(Long followerId, String targetType, Long targetId) {
        log.info("用户 {} 关注 {} {}", followerId, targetType, targetId);

        // 检查是否已经关注
        if (followRepository.existsByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId)) {
            log.warn("用户 {} 已经关注过 {} {}", followerId, targetType, targetId);
            return;
        }

        // 创建关注记录
        Follow follow = new Follow(followerId, targetType, targetId);
        followRepository.save(follow);

        // 更新目标的关注数
        updateTargetFollowCount(targetType, targetId, 1);

        log.info("关注成功，用户: {}, 目标: {} {}", followerId, targetType, targetId);
    }

    @Override
    public void unfollowTarget(Long followerId, String targetType, Long targetId) {
        log.info("用户 {} 取消关注 {} {}", followerId, targetType, targetId);

        // 检查是否已经关注
        if (!followRepository.existsByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId)) {
            log.warn("用户 {} 尚未关注 {} {}", followerId, targetType, targetId);
            return;
        }

        // 删除关注记录
        followRepository.deleteByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId);

        // 更新目标的关注数
        updateTargetFollowCount(targetType, targetId, -1);

        log.info("取消关注成功，用户: {}, 目标: {} {}", followerId, targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTargetFollowedByUser(Long followerId, String targetType, Long targetId) {
        return followRepository.existsByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTargetFollowCount(String targetType, Long targetId) {
        return followRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }





    @Override
    @Transactional(readOnly = true)
    public long getUserFollowingCount(Long followerId, String targetType) {
        return followRepository.countByFollowerIdAndTargetType(followerId, targetType);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUserFollowerCount(Long targetId) {
        return followRepository.countByTargetIdAndTargetType(targetId, "USER");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getUserFollowedUsers(Long followerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findUserFollowedUsers(followerId, pageable);

        List<FollowUserResponse> userList = followPage.getContent().stream()
                .flatMap(follow -> Optional.ofNullable(userMapper.findById(follow.getTargetId()))
                        .map(user -> new FollowUserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getNickname(),
                                user.getEmail(),
                                user.getAvatarUrl(),
                                user.getBio(),
                                follow.getCreatedAt()))
                        .map(java.util.stream.Stream::of)
                        .orElseGet(java.util.stream.Stream::empty))
                .collect(Collectors.toList());

        return new PageImpl<>(userList, pageable, followPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getUserFollowers(Long targetId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findUserFollowers(targetId, pageable);

        List<FollowUserResponse> userList = followPage.getContent().stream()
                .flatMap(follow -> Optional.ofNullable(userMapper.findById(follow.getFollowerId()))
                        .map(user -> new FollowUserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getNickname(),
                                user.getEmail(),
                                user.getAvatarUrl(),
                                user.getBio(),
                                follow.getCreatedAt()))
                        .map(java.util.stream.Stream::of)
                        .orElseGet(java.util.stream.Stream::empty))
                .collect(Collectors.toList());

        return new PageImpl<>(userList, pageable, followPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowQuestionResponse> getUserFollowedQuestions(Long followerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findUserFollowedQuestions(followerId, pageable);

        List<FollowQuestionResponse> questionList = followPage.getContent().stream()
                .flatMap(follow -> questionRepository.findById(follow.getTargetId())
                        .map(question -> new FollowQuestionResponse(
                                question.getId(),
                                question.getTitle(),
                                question.getContent(),
                                question.getAuthorId(),
                                question.getStatus(),
                                question.getViewCount(),
                                question.getLikeCount(),
                                question.getFollowCount(),
                                question.getCommentCount(),
                                question.getCreatedAt(),
                                follow.getCreatedAt()))
                        .map(java.util.stream.Stream::of)
                        .orElseGet(java.util.stream.Stream::empty))
                .collect(Collectors.toList());

        return new PageImpl<>(questionList, pageable, followPage.getTotalElements());
    }

    /**
     * 更新目标的关注数
     */
    private void updateTargetFollowCount(String targetType, Long targetId, int delta) {
        switch (targetType) {
            case "QUESTION":
                Question question = questionRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("问答不存在"));
                if (delta > 0) {
                    question.incrementFollowCount();
                } else {
                    question.decrementFollowCount();
                }
                questionRepository.save(question);
                break;
            case "USER":
                // 用户关注不需要更新计数，因为用户实体中没有关注数字段
                break;
            default:
                log.warn("未知的目标类型: {}", targetType);
        }
    }
}
