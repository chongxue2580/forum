package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.comment.CommentCreateRequest;
import com.example.blog_froum.dto.comment.CommentResponse;
import com.example.blog_froum.dto.statistics.CommentStatisticsResponse;
import com.example.blog_froum.entity.Article;
import com.example.blog_froum.entity.Comment;
import com.example.blog_froum.entity.Question;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.service.CommentService;
import com.example.blog_froum.service.LikeService;
import com.example.blog_froum.service.NotificationService;
import com.example.blog_froum.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private LikeService likeService;

    @Autowired
    private NotificationService notificationService;



    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Override
    public CommentResponse createComment(Long userId, CommentCreateRequest request) {
        log.info("用户 {} 创建评论，目标类型: {}, 目标ID: {}", userId, request.getTargetType(), request.getTargetId());

        // 验证用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userService.assertCanCreateContent(userId);

        // 验证目标是否存在
        Long targetAuthorId = null;
        String targetTitle = null;

        if ("ARTICLE".equals(request.getTargetType())) {
            Article article = articleRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new RuntimeException("文章不存在"));
            targetAuthorId = article.getAuthorId();
            targetTitle = article.getTitle();
        } else if ("QUESTION".equals(request.getTargetType())) {
            Question question = questionRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new RuntimeException("问答不存在"));
            targetAuthorId = question.getAuthorId();
            targetTitle = question.getTitle();
        } else {
            throw new RuntimeException("不支持的目标类型");
        }

        // 如果是回复评论，验证父评论是否存在
        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));

            // 确保父评论属于同一目标
            if (!parentComment.getTargetType().equals(request.getTargetType()) ||
                !parentComment.getTargetId().equals(request.getTargetId())) {
                throw new RuntimeException("父评论不属于该目标");
            }
        }

        // 创建评论
        Comment comment = new Comment();
        comment.setTargetType(request.getTargetType());
        comment.setTargetId(request.getTargetId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setIsDeleted(false);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        savedComment.setUser(user);

        // 更新目标的评论数
        if ("ARTICLE".equals(request.getTargetType())) {
            Article article = articleRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new RuntimeException("文章不存在"));
            article.incrementCommentCount();
            articleRepository.save(article);
        } else if ("QUESTION".equals(request.getTargetType())) {
            Question question = questionRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new RuntimeException("问答不存在"));
            question.incrementCommentCount();
            questionRepository.save(question);
        }

        // 创建评论通知
        if (!userId.equals(targetAuthorId)) {
            if ("ARTICLE".equals(request.getTargetType())) {
                notificationService.createArticleCommentNotification(
                    targetAuthorId, userId, request.getTargetId(), targetTitle);
            } else if ("QUESTION".equals(request.getTargetType())) {
                notificationService.createQuestionCommentNotification(
                    targetAuthorId, userId, request.getTargetId(), targetTitle);
            }
        }

        log.info("评论创建成功，ID: {}", savedComment.getId());
        return CommentResponse.fromEntity(savedComment);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        log.info("用户 {} 删除评论 {}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 验证权限
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此评论");
        }

        // 软删除
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // 更新目标的评论数
        if ("ARTICLE".equals(comment.getTargetType())) {
            Article article = articleRepository.findById(comment.getTargetId())
                    .orElseThrow(() -> new RuntimeException("文章不存在"));
            article.decrementCommentCount();
            articleRepository.save(article);
        } else if ("QUESTION".equals(comment.getTargetType())) {
            Question question = questionRepository.findById(comment.getTargetId())
                    .orElseThrow(() -> new RuntimeException("问答不存在"));
            question.decrementCommentCount();
            if (Boolean.TRUE.equals(comment.getIsBestAnswer())) {
                question.setIsSolved(false);
            }
            questionRepository.save(question);
        }

        log.info("评论删除成功，ID: {}", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new RuntimeException("评论不存在");
        }

        CommentResponse response = CommentResponse.fromEntity(comment);

        if (comment.getParentId() == null) {
            List<CommentResponse> replies = commentRepository
                    .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(comment.getId())
                    .stream()
                    .map(CommentResponse::fromEntity)
                    .collect(Collectors.toList());
            response.setReplies(replies);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getArticleComments(Long articleId, Pageable pageable) {
        return getTargetComments("ARTICLE", articleId, pageable);
    }

    /**
     * 获取问答评论
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getQuestionComments(Long questionId, Pageable pageable) {
        return getTargetComments("QUESTION", questionId, pageable);
    }

    /**
     * 通用方法：获取目标的评论
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getTargetComments(String targetType, Long targetId, Pageable pageable) {
        // 获取顶级评论
        Page<Comment> commentPage = commentRepository
                .findByTargetTypeAndTargetIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
                    targetType, targetId, pageable);

        return commentPage.map(comment -> {
            CommentResponse response = CommentResponse.fromEntity(comment);

            // 获取子评论
            List<Comment> replies = commentRepository
                    .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(comment.getId());

            List<CommentResponse> replyResponses = replies.stream()
                    .map(CommentResponse::fromEntity)
                    .collect(Collectors.toList());

            response.setReplies(replyResponses);
            return response;
        });
    }

    // 管理员功能实现
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getAllComments(Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        return commentPage.map(CommentResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByTargetType(String targetType, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByTargetTypeAndIsDeletedFalseOrderByCreatedAtDesc(targetType, pageable);
        return commentPage.map(CommentResponse::fromEntity);
    }

    @Override
    public void adminDeleteComment(Long commentId) {
        log.info("管理员删除评论，ID: {}", commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            return;
        }

        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        decrementTargetCommentCount(comment);
        log.info("管理员删除评论成功，ID: {}", commentId);
    }

    @Override
    public void adminRestoreComment(Long commentId) {
        log.info("管理员恢复评论，ID: {}", commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (!Boolean.TRUE.equals(comment.getIsDeleted())) {
            return;
        }

        comment.setIsDeleted(false);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        incrementTargetCommentCount(comment);
        log.info("管理员恢复评论成功，ID: {}", commentId);
    }

    @Override
    public void batchDeleteComments(java.util.List<Long> commentIds) {
        log.info("管理员批量删除评论，数量: {}", commentIds.size());

        List<Long> failedCommentIds = new ArrayList<>();
        for (Long commentId : commentIds) {
            try {
                adminDeleteComment(commentId);
            } catch (Exception e) {
                log.error("删除评论失败，ID: {}", commentId, e);
                failedCommentIds.add(commentId);
            }
        }

        if (!failedCommentIds.isEmpty()) {
            throw new RuntimeException("批量删除评论部分失败，失败ID: " + failedCommentIds);
        }

        log.info("批量删除评论完成");
    }

    @Override
    @Transactional(readOnly = true)
    public CommentStatisticsResponse getCommentStatistics() {
        log.info("获取评论统计信息");

        // 统计评论数量
        long totalComments = commentRepository.countByIsDeletedFalse();
        long articleComments = commentRepository.countByTargetTypeAndIsDeletedFalse("ARTICLE");
        long questionComments = commentRepository.countByTargetTypeAndIsDeletedFalse("QUESTION");

        return new CommentStatisticsResponse(totalComments, articleComments, questionComments);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getRecentComments(Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        return commentPage.map(CommentResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> searchCommentsForAdmin(String targetType,
                                                        Long targetId,
                                                        String keyword,
                                                        LocalDateTime startTime,
                                                        LocalDateTime endTime,
                                                        Boolean deleted,
                                                        Pageable pageable) {
        String normalizedType = StringUtils.hasText(targetType) ? targetType.trim().toUpperCase() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return commentRepository
                .searchForAdmin(normalizedType, targetId, normalizedKeyword, startTime, endTime, deleted, pageable)
                .map(CommentResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getUserComments(Long userId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
        
        return commentPage.map(CommentResponse::fromEntity);
    }

    @Override
    public void likeComment(Long commentId, Long userId) {
        log.info("用户 {} 点赞评论 {}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 使用LikeService处理点赞
        likeService.likeTarget(userId, "COMMENT", commentId);

        // 创建点赞通知
        if (!comment.getUserId().equals(userId)) {
            notificationService.createCommentLikeNotification(
                comment.getUserId(), userId, commentId, comment.getContent());
        }
    }

    @Override
    public void unlikeComment(Long commentId, Long userId) {
        log.info("用户 {} 取消点赞评论 {}", userId, commentId);
        
        commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        likeService.unlikeTarget(userId, "COMMENT", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByTarget(String targetType, Long targetId, Pageable pageable) {
        log.info("获取目标类型 {} 目标ID {} 的评论列表", targetType, targetId);

        Page<Comment> commentPage = commentRepository
                .findByTargetTypeAndTargetIdAndIsDeletedFalseOrderByCreatedAtDesc(targetType, targetId, pageable);

        return commentPage.map(CommentResponse::fromEntity);
    }

    private void incrementTargetCommentCount(Comment comment) {
        if ("ARTICLE".equals(comment.getTargetType())) {
            articleRepository.findById(comment.getTargetId()).ifPresent(article -> {
                article.incrementCommentCount();
                articleRepository.save(article);
            });
        } else if ("QUESTION".equals(comment.getTargetType())) {
            questionRepository.findById(comment.getTargetId()).ifPresent(question -> {
                question.incrementCommentCount();
                questionRepository.save(question);
            });
        }
    }

    private void decrementTargetCommentCount(Comment comment) {
        // 目标文章/问答可能已被删除（如作者内容清理），此处容错处理，不影响评论删除本身
        if ("ARTICLE".equals(comment.getTargetType())) {
            articleRepository.findById(comment.getTargetId()).ifPresent(article -> {
                article.decrementCommentCount();
                articleRepository.save(article);
            });
        } else if ("QUESTION".equals(comment.getTargetType())) {
            questionRepository.findById(comment.getTargetId()).ifPresent(question -> {
                question.decrementCommentCount();
                if (Boolean.TRUE.equals(comment.getIsBestAnswer())) {
                    question.setIsSolved(false);
                }
                questionRepository.save(question);
            });
        }
    }
}
