package com.example.blog_froum.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 管理员首次绑定两步验证确认请求DTO
 */
@Data
public class AdminTwoFactorSetupConfirmRequest {
    @NotBlank(message = "绑定令牌不能为空")
    private String setupToken;

    @NotBlank(message = "两步验证码不能为空")
    private String code;
}
