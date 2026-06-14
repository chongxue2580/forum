package com.example.blog_froum.dto.notification;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 站内私信请求DTO
 */
@Data
public class DirectMessageRequest {
    @NotNull(message = "接收用户不能为空")
    private Long toUserId;

    @NotBlank(message = "私信内容不能为空")
    @Size(max = 1000, message = "私信内容不能超过1000个字符")
    private String content;
}
