package com.example.blog_froum.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 用户批量操作请求
 */
@Data
public class BatchUserRequest {
    private List<Long> ids;
    private String reason;
}
