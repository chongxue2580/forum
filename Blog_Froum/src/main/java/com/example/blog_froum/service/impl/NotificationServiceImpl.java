package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.notification.NotificationResponse;
import com.example.blog_froum.entity.Notification;
import com.example.blog_froum.repository.NotificationRepository;
import com.example.blog_froum.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 */
@Slf4j
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void createNotification(Long userId, Long fromUserId, String type, String title, String content, Long targetId) {
        log.info("创建通知，接收者: {}, 发送者: {}, 类型: {}", userId, fromUserId, type);

        // 不给自己发送通知
        if (userId.equals(fromUserId)) {
            return;
        }

        Notification notification = new Notification(userId, fromUserId, type, title, content, targetId);
        notificationRepository.save(notification);

        log.info("通知创建成功，ID: {}", notification.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notificationPage.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserUnreadNotifications(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        return notificationPage.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserReadNotifications(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndIsReadTrueOrderByCreatedAtDesc(userId, pageable);
        return notificationPage.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotificationsByType(Long userId, String type, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        return notificationPage.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUserUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markNotificationAsRead(Long notificationId, Long userId) {
        log.info("标记通知为已读，通知ID: {}, 用户ID: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        // 检查权限
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作此通知");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Override
    public void markNotificationsAsRead(List<Long> notificationIds, Long userId) {
        log.info("批量标记通知为已读，用户ID: {}, 通知数量: {}", userId, notificationIds.size());
        notificationRepository.markAsReadByIds(notificationIds, userId);
    }

    @Override
    public void markAllNotificationsAsRead(Long userId) {
        log.info("标记用户所有通知为已读，用户ID: {}", userId);
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        log.info("删除通知，通知ID: {}, 用户ID: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        // 检查权限
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此通知");
        }

        notificationRepository.delete(notification);
    }

    @Override
    public void deleteAllNotifications(Long userId) {
        log.info("删除用户所有通知，用户ID: {}", userId);
        notificationRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteAllReadNotifications(Long userId) {
        log.info("删除用户所有已读通知，用户ID: {}", userId);
        notificationRepository.deleteByUserIdAndIsReadTrue(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void createArticleCommentNotification(Long articleAuthorId, Long commentAuthorId, Long articleId, String articleTitle) {
        String title = "您的文章收到了新评论";
        String content = String.format("有用户评论了您的文章：%s", articleTitle);
        createNotification(articleAuthorId, commentAuthorId, "ARTICLE_COMMENT", title, content, articleId);
    }

    @Override
    public void createQuestionCommentNotification(Long questionAuthorId, Long commentAuthorId, Long questionId, String questionTitle) {
        String title = "您的问答收到了新评论";
        String content = String.format("有用户评论了您的问答：%s", questionTitle);
        createNotification(questionAuthorId, commentAuthorId, "QUESTION_COMMENT", title, content, questionId);
    }

    @Override
    public void createUserFollowNotification(Long targetUserId, Long followerUserId, String followerUsername) {
        String title = "您有新的粉丝";
        String content = String.format("%s 关注了您", followerUsername);
        createNotification(targetUserId, followerUserId, "USER_FOLLOW", title, content, targetUserId);
    }

    @Override
    public void createQuestionFollowNotification(Long questionAuthorId, Long followerUserId, Long questionId, String questionTitle) {
        String title = "您的问答被关注了";
        String content = String.format("有用户关注了您的问答：%s", questionTitle);
        createNotification(questionAuthorId, followerUserId, "QUESTION_FOLLOW", title, content, questionId);
    }

    @Override
    public void createArticleLikeNotification(Long articleAuthorId, Long likerUserId, Long articleId, String articleTitle) {
        String title = "您的文章被点赞了";
        String content = String.format("有用户点赞了您的文章：%s", articleTitle);
        createNotification(articleAuthorId, likerUserId, "ARTICLE_LIKE", title, content, articleId);
    }

    @Override
    public void createQuestionLikeNotification(Long questionAuthorId, Long likerUserId, Long questionId, String questionTitle) {
        String title = "您的问答被点赞了";
        String content = String.format("有用户点赞了您的问答：%s", questionTitle);
        createNotification(questionAuthorId, likerUserId, "QUESTION_LIKE", title, content, questionId);
    }

    @Override
    public void createCommentLikeNotification(Long commentAuthorId, Long likerUserId, Long commentId, String commentContent) {
        String title = "您的评论被点赞了";
        String content = String.format("有用户点赞了您的评论：%s", 
                commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent);
        createNotification(commentAuthorId, likerUserId, "COMMENT_LIKE", title, content, commentId);
    }
}
