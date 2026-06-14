package com.example.blog_froum.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Boolean remember = false;

    private String captchaId;

    private Float captchaPercentage;

    private String twoFactorCode;

    private String twoFactorToken;
}
