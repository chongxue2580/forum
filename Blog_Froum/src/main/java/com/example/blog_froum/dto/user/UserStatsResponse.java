package com.example.blog_froum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户统计响应，字段名保持原接口返回结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private long articles;
    private long questions;
    private long answers;
    private long followers;
    private long following;
}
