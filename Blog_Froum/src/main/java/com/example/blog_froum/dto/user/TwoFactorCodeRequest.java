package com.example.blog_froum.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 两步验证码请求DTO
 */
@Data
public class TwoFactorCodeRequest {
    @NotBlank(message = "两步验证码不能为空")
    private String code;
}
