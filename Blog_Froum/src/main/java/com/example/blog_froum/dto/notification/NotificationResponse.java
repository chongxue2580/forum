package com.example.blog_froum.dto.notification;

import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.Notification;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应DTO
 */
@Data
@ApiModel(description = "通知响应")
public class NotificationResponse {

    @ApiModelProperty(value = "通知ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "发送者信息")
    private UserResponse fromUser;

    @ApiModelProperty(value = "通知类型", example = "ARTICLE_COMMENT")
    private String type;

    @ApiModelProperty(value = "通知标题", example = "您的文章收到了新评论")
    private String title;

    @ApiModelProperty(value = "通知内容", example = "张三评论了您的文章：Spring Boot入门指南")
    private String content;

    @ApiModelProperty(value = "目标ID", example = "1")
    private Long targetId;

    @ApiModelProperty(value = "是否已读", example = "false")
    private Boolean isRead;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应DTO
     */
    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setTargetId(notification.getTargetId());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());

        // 设置发送者信息
        if (notification.getFromUser() != null) {
            response.setFromUser(UserResponse.fromUser(notification.getFromUser()));
        }

        return response;
    }
}
