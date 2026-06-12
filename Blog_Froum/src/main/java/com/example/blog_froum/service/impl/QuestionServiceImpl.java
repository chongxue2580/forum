package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.question.QuestionCreateRequest;
import com.example.blog_froum.dto.question.QuestionResponse;
import com.example.blog_froum.dto.question.QuestionUpdateRequest;
import com.example.blog_froum.dto.statistics.QuestionStatisticsResponse;
import com.example.blog_froum.entity.Comment;
import com.example.blog_froum.entity.Question;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.FollowRepository;
import com.example.blog_froum.repository.LikeRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.service.FollowService;
import com.example.blog_froum.service.LikeService;
import com.example.blog_froum.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 问答服务实现类
 */
@Slf4j
@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Override
    public QuestionResponse createQuestion(Long authorId, QuestionCreateRequest request) {
        log.info("创建问答，作者ID: {}, 标题: {}", authorId, request.getTitle());

        // 验证作者是否存在
        User author = userMapper.findById(authorId);
        if (author == null) {
            throw new RuntimeException("作者不存在");
        }

        // 创建问答实体
        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setAuthorId(authorId);
        question.setStatus("APPROVED"); // 直接设为已审核状态，便于开发测试

        // 保存问答
        Question savedQuestion = questionRepository.save(question);
        log.info("问答创建成功，ID: {}", savedQuestion.getId());

        return QuestionResponse.fromEntity(savedQuestion);
    }

    @Override
    public QuestionResponse updateQuestion(Long questionId, Long authorId, QuestionUpdateRequest request) {
        log.info("更新问答，ID: {}, 作者ID: {}", questionId, authorId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        // 检查权限
        if (!question.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限修改此问答");
        }

        // 更新问答信息
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());

        Question updatedQuestion = questionRepository.save(question);
        log.info("问答更新成功，ID: {}", updatedQuestion.getId());

        return QuestionResponse.fromEntity(updatedQuestion);
    }

    @Override
    public void deleteQuestion(Long questionId, Long authorId) {
        log.info("删除问答，ID: {}, 作者ID: {}", questionId, authorId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        // 检查权限
        if (!question.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限删除此问答");
        }

        questionRepository.delete(question);
        log.info("问答删除成功，ID: {}", questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findByIdWithAuthor(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        return QuestionResponse.fromEntity(question);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findAll(pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByStatus(String status, Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusWithAuthorOrderByCreatedAtDesc(status, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getApprovedQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusWithAuthorOrderByCreatedAtDesc("APPROVED", pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> searchPublicQuestions(Long tagId,
                                                        Boolean solved,
                                                        String keyword,
                                                        String sort,
                                                        Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedSort = StringUtils.hasText(sort) ? sort.trim() : "newest";
        Page<Question> questionPage = questionRepository.searchPublicQuestions(
                "APPROVED",
                tagId,
                solved,
                normalizedKeyword,
                normalizedSort,
                pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getUserQuestions(Long authorId, Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getUserQuestionsByStatus(Long authorId, String status, Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusAndAuthorIdOrderByCreatedAtDesc(
                status, authorId, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getPopularQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusOrderByLikeCountDescCreatedAtDesc("APPROVED", pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getSolvedQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusAndIsSolvedOrderByCreatedAtDesc("APPROVED", true, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getUnsolvedQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusAndIsSolvedOrderByCreatedAtDesc("APPROVED", false, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getPinnedQuestions() {
        List<Question> questions = questionRepository.findByStatusAndIsPinnedOrderByCreatedAtDesc("APPROVED", true);
        return questions.stream()
                .map(QuestionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getFeaturedQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findByStatusAndIsFeaturedOrderByCreatedAtDesc("APPROVED", true, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> searchQuestions(String keyword, Pageable pageable) {
        Page<Question> questionPage = questionRepository.searchByKeyword("APPROVED", keyword, pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    @Override
    public void likeQuestion(Long questionId, Long userId) {
        log.info("用户 {} 点赞问答 {}", userId, questionId);
        likeService.likeTarget(userId, "QUESTION", questionId);
    }

    @Override
    public void unlikeQuestion(Long questionId, Long userId) {
        log.info("用户 {} 取消点赞问答 {}", userId, questionId);
        likeService.unlikeTarget(userId, "QUESTION", questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isQuestionLikedByUser(Long questionId, Long userId) {
        return likeService.isTargetLikedByUser(userId, "QUESTION", questionId);
    }

    @Override
    public void followQuestion(Long questionId, Long userId) {
        log.info("用户 {} 关注问答 {}", userId, questionId);
        followService.followTarget(userId, "QUESTION", questionId);
    }

    @Override
    public void unfollowQuestion(Long questionId, Long userId) {
        log.info("用户 {} 取消关注问答 {}", userId, questionId);
        followService.unfollowTarget(userId, "QUESTION", questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isQuestionFollowedByUser(Long questionId, Long userId) {
        return followService.isTargetFollowedByUser(userId, "QUESTION", questionId);
    }

    @Override
    public void markQuestionAsSolved(Long questionId, Long authorId) {
        log.info("标记问答为已解决，ID: {}, 作者ID: {}", questionId, authorId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        // 检查权限
        if (!question.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限修改此问答状态");
        }

        question.setIsSolved(true);
        questionRepository.save(question);
    }

    @Override
    public void markQuestionAsUnsolved(Long questionId, Long authorId) {
        log.info("标记问答为未解决，ID: {}, 作者ID: {}", questionId, authorId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        // 检查权限
        if (!question.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限修改此问答状态");
        }

        question.setIsSolved(false);
        questionRepository.save(question);
    }

    @Override
    public QuestionResponse setBestAnswer(Long questionId, Long commentId, Long authorId) {
        log.info("设置最佳答案，问答ID: {}, 评论ID: {}, 作者ID: {}", questionId, commentId, authorId);

        Question question = getOwnedQuestion(questionId, authorId);
        Comment answer = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("回答不存在"));

        if (Boolean.TRUE.equals(answer.getIsDeleted())) {
            throw new RuntimeException("回答不存在");
        }
        if (!"QUESTION".equals(answer.getTargetType()) || !questionId.equals(answer.getTargetId())) {
            throw new RuntimeException("回答不属于该问答");
        }
        if (answer.getParentId() != null) {
            throw new RuntimeException("只能采纳顶层回答");
        }

        List<Comment> currentBestAnswers = commentRepository
                .findByTargetTypeAndTargetIdAndIsBestAnswerTrueAndIsDeletedFalse("QUESTION", questionId);
        currentBestAnswers.forEach(comment -> comment.setIsBestAnswer(false));
        if (!currentBestAnswers.isEmpty()) {
            commentRepository.saveAll(currentBestAnswers);
        }

        answer.setIsBestAnswer(true);
        commentRepository.save(answer);

        question.setIsSolved(true);
        Question savedQuestion = questionRepository.save(question);
        return QuestionResponse.fromEntity(savedQuestion);
    }

    @Override
    public QuestionResponse unsetBestAnswer(Long questionId, Long authorId) {
        log.info("取消最佳答案，问答ID: {}, 作者ID: {}", questionId, authorId);

        Question question = getOwnedQuestion(questionId, authorId);
        List<Comment> currentBestAnswers = commentRepository
                .findByTargetTypeAndTargetIdAndIsBestAnswerTrueAndIsDeletedFalse("QUESTION", questionId);
        currentBestAnswers.forEach(comment -> comment.setIsBestAnswer(false));
        if (!currentBestAnswers.isEmpty()) {
            commentRepository.saveAll(currentBestAnswers);
        }

        question.setIsSolved(false);
        Question savedQuestion = questionRepository.save(question);
        return QuestionResponse.fromEntity(savedQuestion);
    }

    @Override
    public void incrementViewCount(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.incrementViewCount();
        questionRepository.save(question);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getFollowedQuestions(Long userId, Pageable pageable) {
        Page<Question> questionPage = questionRepository.findFollowedQuestions(userId, "APPROVED", pageable);
        return questionPage.map(QuestionResponse::fromEntity);
    }

    private Question getOwnedQuestion(Long questionId, Long authorId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        if (!question.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限修改此问答状态");
        }

        return question;
    }

    // 管理员功能实现
    @Override
    public void approveQuestion(Long questionId) {
        log.info("管理员审核通过问答，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setStatus("APPROVED");
        questionRepository.save(question);
        log.info("问答审核通过成功，ID: {}", questionId);
    }

    @Override
    public void rejectQuestion(Long questionId, String reason) {
        log.info("管理员审核拒绝问答，ID: {}, 原因: {}", questionId, reason);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setStatus("REJECTED");
        questionRepository.save(question);
        log.info("问答审核拒绝成功，ID: {}", questionId);
    }

    @Override
    public void pinQuestion(Long questionId) {
        log.info("管理员设置问答置顶，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setIsPinned(true);
        questionRepository.save(question);
        log.info("问答设置置顶成功，ID: {}", questionId);
    }

    @Override
    public void unpinQuestion(Long questionId) {
        log.info("管理员取消问答置顶，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setIsPinned(false);
        questionRepository.save(question);
        log.info("问答取消置顶成功，ID: {}", questionId);
    }

    @Override
    public void featureQuestion(Long questionId) {
        log.info("管理员设置问答精选，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setIsFeatured(true);
        questionRepository.save(question);
        log.info("问答设置精选成功，ID: {}", questionId);
    }

    @Override
    public void unfeatureQuestion(Long questionId) {
        log.info("管理员取消问答精选，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setIsFeatured(false);
        questionRepository.save(question);
        log.info("问答取消精选成功，ID: {}", questionId);
    }

    @Override
    public void adminMarkQuestionAsSolved(Long questionId) {
        log.info("管理员标记问答为已解决，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setIsSolved(true);
        questionRepository.save(question);
        log.info("管理员标记问答为已解决成功，ID: {}", questionId);
    }

    @Override
    public void adminMarkQuestionAsUnsolved(Long questionId) {
        log.info("管理员标记问答为未解决，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        question.setIsSolved(false);
        questionRepository.save(question);
        log.info("管理员标记问答为未解决成功，ID: {}", questionId);
    }

    @Override
    public void adminDeleteQuestion(Long questionId) {
        log.info("管理员删除问答，ID: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问答不存在"));

        questionRepository.delete(question);
        log.info("管理员删除问答成功，ID: {}", questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionStatisticsResponse getQuestionStatistics() {
        log.info("获取问答统计信息");

        // 统计各状态的问答数量
        long totalQuestions = questionRepository.count();
        long approvedQuestions = questionRepository.countByStatus("APPROVED");
        long pendingQuestions = questionRepository.countByStatus("PENDING");
        long rejectedQuestions = questionRepository.countByStatus("REJECTED");
        long solvedQuestions = questionRepository.countByStatusAndIsSolved("APPROVED", true);
        long pinnedQuestions = questionRepository.countByStatusAndIsPinned("APPROVED", true);
        long featuredQuestions = questionRepository.countByStatusAndIsFeatured("APPROVED", true);

        return new QuestionStatisticsResponse(
                totalQuestions,
                approvedQuestions,
                pendingQuestions,
                rejectedQuestions,
                solvedQuestions,
                pinnedQuestions,
                featuredQuestions);
    }
}
