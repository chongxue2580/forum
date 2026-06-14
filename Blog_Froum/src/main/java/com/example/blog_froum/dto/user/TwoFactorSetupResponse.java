package com.example.blog_froum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 两步验证绑定信息响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {
    private Boolean enabled;
    private String secret;
    private String otpAuthUrl;
}
