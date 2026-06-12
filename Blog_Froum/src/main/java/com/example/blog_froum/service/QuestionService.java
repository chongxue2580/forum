package com.example.blog_froum.service;

import com.example.blog_froum.dto.question.QuestionCreateRequest;
import com.example.blog_froum.dto.question.QuestionResponse;
import com.example.blog_froum.dto.question.QuestionUpdateRequest;
import com.example.blog_froum.dto.statistics.QuestionStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 问答服务接口
 */
public interface QuestionService {

    /**
     * 创建问答
     */
    QuestionResponse createQuestion(Long authorId, QuestionCreateRequest request);

    /**
     * 更新问答
     */
    QuestionResponse updateQuestion(Long questionId, Long authorId, QuestionUpdateRequest request);

    /**
     * 删除问答
     */
    void deleteQuestion(Long questionId, Long authorId);

    /**
     * 根据ID获取问答详情
     */
    QuestionResponse getQuestionById(Long questionId);

    /**
     * 分页获取问答列表
     */
    Page<QuestionResponse> getQuestions(Pageable pageable);

    /**
     * 分页获取指定状态的问答列表
     */
    Page<QuestionResponse> getQuestionsByStatus(String status, Pageable pageable);

    /**
     * 分页获取已批准的问答列表
     */
    Page<QuestionResponse> getApprovedQuestions(Pageable pageable);

    /**
     * 公共问答列表筛选。
     */
    Page<QuestionResponse> searchPublicQuestions(Long tagId,
                                                 Boolean solved,
                                                 String keyword,
                                                 String sort,
                                                 Pageable pageable);

    /**
     * 分页获取用户的问答列表
     */
    Page<QuestionResponse> getUserQuestions(Long authorId, Pageable pageable);

    /**
     * 获取指定状态下用户发布的问答列表
     */
    Page<QuestionResponse> getUserQuestionsByStatus(Long authorId, String status, Pageable pageable);

    /**
     * 分页获取热门问答
     */
    Page<QuestionResponse> getPopularQuestions(Pageable pageable);

    /**
     * 分页获取已解决的问答
     */
    Page<QuestionResponse> getSolvedQuestions(Pageable pageable);

    /**
     * 分页获取未解决的问答
     */
    Page<QuestionResponse> getUnsolvedQuestions(Pageable pageable);

    /**
     * 获取置顶问答
     */
    List<QuestionResponse> getPinnedQuestions();

    /**
     * 分页获取精选问答
     */
    Page<QuestionResponse> getFeaturedQuestions(Pageable pageable);

    /**
     * 搜索问答
     */
    Page<QuestionResponse> searchQuestions(String keyword, Pageable pageable);

    /**
     * 点赞问答
     */
    void likeQuestion(Long questionId, Long userId);

    /**
     * 取消点赞问答
     */
    void unlikeQuestion(Long questionId, Long userId);

    /**
     * 检查用户是否已点赞问答
     */
    boolean isQuestionLikedByUser(Long questionId, Long userId);

    /**
     * 关注问答
     */
    void followQuestion(Long questionId, Long userId);

    /**
     * 取消关注问答
     */
    void unfollowQuestion(Long questionId, Long userId);

    /**
     * 检查用户是否已关注问答
     */
    boolean isQuestionFollowedByUser(Long questionId, Long userId);

    /**
     * 标记问答为已解决
     */
    void markQuestionAsSolved(Long questionId, Long authorId);

    /**
     * 标记问答为未解决
     */
    void markQuestionAsUnsolved(Long questionId, Long authorId);

    /**
     * 设置最佳答案
     */
    QuestionResponse setBestAnswer(Long questionId, Long commentId, Long authorId);

    /**
     * 取消最佳答案
     */
    QuestionResponse unsetBestAnswer(Long questionId, Long authorId);

    /**
     * 增加问答浏览量
     */
    void incrementViewCount(Long questionId);

    /**
     * 获取用户关注的问答
     */
    Page<QuestionResponse> getFollowedQuestions(Long userId, Pageable pageable);

    // 管理员功能
    /**
     * 审核通过问答
     */
    void approveQuestion(Long questionId);

    /**
     * 审核拒绝问答
     */
    void rejectQuestion(Long questionId, String reason);

    /**
     * 设置问答为置顶
     */
    void pinQuestion(Long questionId);

    /**
     * 取消问答置顶
     */
    void unpinQuestion(Long questionId);

    /**
     * 设置问答为精选
     */
    void featureQuestion(Long questionId);

    /**
     * 取消问答精选
     */
    void unfeatureQuestion(Long questionId);

    /**
     * 管理员标记问答为已解决
     */
    void adminMarkQuestionAsSolved(Long questionId);

    /**
     * 管理员标记问答为未解决
     */
    void adminMarkQuestionAsUnsolved(Long questionId);

    /**
     * 管理员删除问答
     */
    void adminDeleteQuestion(Long questionId);

    /**
     * 获取问答统计信息
     */
    QuestionStatisticsResponse getQuestionStatistics();
}
