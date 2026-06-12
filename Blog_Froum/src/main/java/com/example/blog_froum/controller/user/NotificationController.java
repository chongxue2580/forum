package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.notification.NotificationResponse;
import com.example.blog_froum.service.NotificationService;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8081"})
@Api(tags = "通知管理")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取用户通知列表
     */
    @GetMapping
    @ApiOperation(value = "获取用户通知列表", notes = "分页获取当前用户的通知列表")
    public Result<Page<NotificationResponse>> getUserNotifications(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的通知列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<NotificationResponse> responsePage = notificationService.getUserNotifications(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户通知列表失败", e);
            return Result.error("获取通知列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户未读通知列表
     */
    @GetMapping("/unread")
    @ApiOperation(value = "获取用户未读通知列表", notes = "分页获取当前用户的未读通知列表")
    public Result<Page<NotificationResponse>> getUserUnreadNotifications(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的未读通知列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<NotificationResponse> responsePage = notificationService.getUserUnreadNotifications(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户未读通知列表失败", e);
            return Result.error("获取未读通知列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户已读通知列表
     */
    @GetMapping("/read")
    @ApiOperation(value = "获取用户已读通知列表", notes = "分页获取当前用户的已读通知列表")
    public Result<Page<NotificationResponse>> getUserReadNotifications(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的已读通知列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<NotificationResponse> responsePage = notificationService.getUserReadNotifications(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户已读通知列表失败", e);
            return Result.error("获取已读通知列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据类型获取用户通知
     */
    @GetMapping("/type/{type}")
    @ApiOperation(value = "根据类型获取用户通知", notes = "根据通知类型分页获取当前用户的通知列表")
    public Result<Page<NotificationResponse>> getUserNotificationsByType(
            @ApiParam(value = "通知类型", required = true) @PathVariable String type,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的 {} 类型通知列表", userId, type);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<NotificationResponse> responsePage = notificationService.getUserNotificationsByType(userId, type, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("根据类型获取用户通知列表失败", e);
            return Result.error("获取通知列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户未读通知数量
     */
    @GetMapping("/unread/count")
    @ApiOperation(value = "获取用户未读通知数量", notes = "获取当前用户的未读通知数量")
    public Result<Long> getUserUnreadNotificationCount() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的未读通知数量", userId);
            long count = notificationService.getUserUnreadNotificationCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取用户未读通知数量失败", e);
            return Result.error("获取未读通知数量失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近的通知
     */
    @GetMapping("/recent")
    @ApiOperation(value = "获取最近的通知", notes = "获取当前用户最近的10条通知")
    public Result<List<NotificationResponse>> getRecentNotifications() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的最近通知", userId);
            List<NotificationResponse> responseList = notificationService.getRecentNotifications(userId);
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取最近通知失败", e);
            return Result.error("获取最近通知失败: " + e.getMessage());
        }
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/{id}/read")
    @ApiOperation(value = "标记通知为已读", notes = "将指定通知标记为已读")
    public Result<Void> markNotificationAsRead(
            @ApiParam(value = "通知ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 标记通知 {} 为已读", userId, id);
            notificationService.markNotificationAsRead(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记通知为已读失败", e);
            return Result.error("标记失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记通知为已读
     */
    @PostMapping("/read")
    @ApiOperation(value = "批量标记通知为已读", notes = "批量将指定通知标记为已读")
    public Result<Void> markNotificationsAsRead(
            @ApiParam(value = "通知ID列表", required = true) @RequestBody List<Long> notificationIds) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 批量标记通知为已读，数量: {}", userId, notificationIds.size());
            notificationService.markNotificationsAsRead(notificationIds, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("批量标记通知为已读失败", e);
            return Result.error("批量标记失败: " + e.getMessage());
        }
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    @ApiOperation(value = "标记所有通知为已读", notes = "将当前用户的所有通知标记为已读")
    public Result<Void> markAllNotificationsAsRead() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 标记所有通知为已读", userId);
            notificationService.markAllNotificationsAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记所有通知为已读失败", e);
            return Result.error("全部标记失败: " + e.getMessage());
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除通知", notes = "删除指定的通知")
    public Result<Void> deleteNotification(
            @ApiParam(value = "通知ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 删除通知 {}", userId, id);
            notificationService.deleteNotification(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除通知失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除所有通知
     */
    @DeleteMapping("/all")
    @ApiOperation(value = "删除所有通知", notes = "删除当前用户的所有通知")
    public Result<Void> deleteAllNotifications() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 删除所有通知", userId);
            notificationService.deleteAllNotifications(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除所有通知失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除所有已读通知
     */
    @DeleteMapping("/read")
    @ApiOperation(value = "删除所有已读通知", notes = "删除当前用户的所有已读通知")
    public Result<Void> deleteAllReadNotifications() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 删除所有已读通知", userId);
            notificationService.deleteAllReadNotifications(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除所有已读通知失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
