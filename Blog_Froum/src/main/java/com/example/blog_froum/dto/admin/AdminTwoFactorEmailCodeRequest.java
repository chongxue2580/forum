package com.example.blog_froum.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 管理员两步验证邮箱验证码请求DTO
 */
@Data
public class AdminTwoFactorEmailCodeRequest {
    @NotBlank(message = "两步验证令牌不能为空")
    private String twoFactorToken;
}
