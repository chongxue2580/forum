package com.example.blog_froum.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ForgotPasswordCodeRequest {

    @NotBlank(message = "用户名或邮箱不能为空")
    private String account;
}
