package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 获取用户的通知列表
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 获取用户的未读通知列表
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 获取用户的已读通知列表
     */
    Page<Notification> findByUserIdAndIsReadTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据通知类型获取用户通知
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type, Pageable pageable);

    /**
     * 统计用户未读通知数量
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 统计用户总通知数量
     */
    long countByUserId(Long userId);

    /**
     * 批量标记用户通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 批量标记指定通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids AND n.userId = :userId")
    int markAsReadByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 删除用户的所有通知
     */
    void deleteByUserId(Long userId);

    /**
     * 删除用户的已读通知
     */
    void deleteByUserIdAndIsReadTrue(Long userId);

    /**
     * 获取最近的通知
     */
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
