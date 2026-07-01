package com.example.blog_froum.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OAuthCallbackRequest {
    @NotBlank(message = "授权码不能为空")
    private String code;

    @NotBlank(message = "state不能为空")
    private String state;
}
