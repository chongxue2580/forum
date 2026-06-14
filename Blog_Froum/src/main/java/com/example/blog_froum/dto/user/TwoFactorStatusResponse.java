package com.example.blog_froum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 两步验证状态响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorStatusResponse {
    private Boolean enabled;
}
