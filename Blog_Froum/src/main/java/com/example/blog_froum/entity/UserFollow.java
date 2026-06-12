package com.example.blog_froum.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户关注实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 关注者ID（粉丝）
     */
    private Long followerId;
    
    /**
     * 被关注者ID
     */
    private Long followingId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 