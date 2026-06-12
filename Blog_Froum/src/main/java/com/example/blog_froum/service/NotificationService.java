package com.example.blog_froum.service;

import com.example.blog_froum.dto.notification.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     */
    void createNotification(Long userId, Long fromUserId, String type, String title, String content, Long targetId);

    /**
     * 获取用户的通知列表
     */
    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    /**
     * 获取用户的未读通知列表
     */
    Page<NotificationResponse> getUserUnreadNotifications(Long userId, Pageable pageable);

    /**
     * 获取用户的已读通知列表
     */
    Page<NotificationResponse> getUserReadNotifications(Long userId, Pageable pageable);

    /**
     * 根据类型获取用户通知
     */
    Page<NotificationResponse> getUserNotificationsByType(Long userId, String type, Pageable pageable);

    /**
     * 获取用户未读通知数量
     */
    long getUserUnreadNotificationCount(Long userId);

    /**
     * 标记通知为已读
     */
    void markNotificationAsRead(Long notificationId, Long userId);

    /**
     * 批量标记通知为已读
     */
    void markNotificationsAsRead(List<Long> notificationIds, Long userId);

    /**
     * 标记用户所有通知为已读
     */
    void markAllNotificationsAsRead(Long userId);

    /**
     * 删除通知
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * 删除用户所有通知
     */
    void deleteAllNotifications(Long userId);

    /**
     * 删除用户所有已读通知
     */
    void deleteAllReadNotifications(Long userId);

    /**
     * 获取最近的通知
     */
    List<NotificationResponse> getRecentNotifications(Long userId);

    /**
     * 创建文章评论通知
     */
    void createArticleCommentNotification(Long articleAuthorId, Long commentAuthorId, Long articleId, String articleTitle);

    /**
     * 创建问答评论通知
     */
    void createQuestionCommentNotification(Long questionAuthorId, Long commentAuthorId, Long questionId, String questionTitle);

    /**
     * 创建用户关注通知
     */
    void createUserFollowNotification(Long targetUserId, Long followerUserId, String followerUsername);

    /**
     * 创建问答关注通知
     */
    void createQuestionFollowNotification(Long questionAuthorId, Long followerUserId, Long questionId, String questionTitle);

    /**
     * 创建文章点赞通知
     */
    void createArticleLikeNotification(Long articleAuthorId, Long likerUserId, Long articleId, String articleTitle);

    /**
     * 创建问答点赞通知
     */
    void createQuestionLikeNotification(Long questionAuthorId, Long likerUserId, Long questionId, String questionTitle);

    /**
     * 创建评论点赞通知
     */
    void createCommentLikeNotification(Long commentAuthorId, Long likerUserId, Long commentId, String commentContent);
}
