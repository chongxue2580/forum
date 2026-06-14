package com.example.blog_froum.dto.user;

import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户信息响应DTO
 */
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String bio;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime lastLoginTime;
    private Integer loginCount;
    private Boolean twoFactorEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 用户的文章列表
    private List<UserArticleSummaryResponse> articles;

    public UserResponse() {}

    public UserResponse(User user) {
        User source = Objects.requireNonNull(user, "user");
        this.id = source.getId();
        this.username = source.getUsername();
        this.email = source.getEmail();
        this.nickname = source.getNickname();
        this.bio = source.getBio();
        this.avatarUrl = source.getAvatarUrl();
        this.role = source.getRole();
        this.status = source.getStatus();
        this.lastLoginTime = source.getLastLoginTime();
        this.loginCount = source.getLoginCount();
        this.twoFactorEnabled = Boolean.TRUE.equals(source.getTwoFactorEnabled());
        this.createdAt = source.getCreatedAt();
        this.updatedAt = source.getUpdatedAt();
    }

    public static UserResponse fromUser(User user) {
        return new UserResponse(user);
    }
}
