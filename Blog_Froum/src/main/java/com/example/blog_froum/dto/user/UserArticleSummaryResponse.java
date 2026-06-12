package com.example.blog_froum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户主页文章摘要响应，替代动态键值结构以保持类型契约。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserArticleSummaryResponse {
    private Long id;
    private String title;
    private String summary;
    private LocalDateTime createTime;
    private Integer viewCount;
    private Integer likeCount;
}
