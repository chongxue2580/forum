package com.example.blog_froum.service.impl;

import com.example.blog_froum.entity.Article;
import com.example.blog_froum.entity.Comment;
import com.example.blog_froum.entity.Like;
import com.example.blog_froum.entity.Question;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.LikeRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞服务实现类
 */
@Slf4j
@Service
@Transactional
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Override
    public void likeTarget(Long userId, String targetType, Long targetId) {
        log.info("用户 {} 点赞 {} {}", userId, targetType, targetId);

        // 检查是否已经点赞
        if (likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)) {
            log.warn("用户 {} 已经点赞过 {} {}", userId, targetType, targetId);
            return;
        }

        // 创建点赞记录
        Like like = new Like(userId, targetType, targetId);
        likeRepository.save(like);

        // 更新目标的点赞数
        updateTargetLikeCount(targetType, targetId, 1);

        log.info("点赞成功，用户: {}, 目标: {} {}", userId, targetType, targetId);
    }

    @Override
    public void unlikeTarget(Long userId, String targetType, Long targetId) {
        log.info("用户 {} 取消点赞 {} {}", userId, targetType, targetId);

        // 检查是否已经点赞
        if (!likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)) {
            log.warn("用户 {} 尚未点赞 {} {}", userId, targetType, targetId);
            return;
        }

        // 删除点赞记录
        likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);

        // 更新目标的点赞数
        updateTargetLikeCount(targetType, targetId, -1);

        log.info("取消点赞成功，用户: {}, 目标: {} {}", userId, targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTargetLikedByUser(Long userId, String targetType, Long targetId) {
        return likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTargetLikeCount(String targetType, Long targetId) {
        return likeRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Long> getUserLikedArticles(Long userId, Pageable pageable) {
        Page<Like> likePage = likeRepository.findUserLikedArticles(userId, pageable);
        return likePage.map(Like::getTargetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Long> getUserLikedQuestions(Long userId, Pageable pageable) {
        Page<Like> likePage = likeRepository.findUserLikedQuestions(userId, pageable);
        return likePage.map(Like::getTargetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Long> getUserLikedComments(Long userId, Pageable pageable) {
        Page<Like> likePage = likeRepository.findUserLikedComments(userId, pageable);
        return likePage.map(Like::getTargetId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUserTotalLikeCount(Long userId) {
        return likeRepository.countByUserId(userId);
    }

    /**
     * 更新目标的点赞数
     */
    private void updateTargetLikeCount(String targetType, Long targetId, int delta) {
        switch (targetType) {
            case "ARTICLE":
                Article article = articleRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("文章不存在"));
                if (delta > 0) {
                    article.incrementLikeCount();
                } else {
                    article.decrementLikeCount();
                }
                articleRepository.save(article);
                break;
            case "QUESTION":
                Question question = questionRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("问答不存在"));
                if (delta > 0) {
                    question.incrementLikeCount();
                } else {
                    question.decrementLikeCount();
                }
                questionRepository.save(question);
                break;
            case "COMMENT":
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("评论不存在"));
                if (delta > 0) {
                    comment.incrementLikeCount();
                } else {
                    comment.decrementLikeCount();
                }
                commentRepository.save(comment);
                break;
            default:
                log.warn("未知的目标类型: {}", targetType);
        }
    }
}
