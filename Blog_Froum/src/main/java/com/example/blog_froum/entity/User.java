package com.example.blog_froum.entity;

import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserBanType;
import com.example.blog_froum.enums.UserStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 50)
    private String nickname;
    
    @Column(length = 500)
    private String bio;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    @Column(name = "login_count")
    private Integer loginCount;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled;

    @Column(name = "two_factor_secret", length = 64)
    private String twoFactorSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "ban_type", length = 20)
    private UserBanType banType;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    @Column(name = "ban_expires_at")
    private LocalDateTime banExpiresAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "banned_by")
    private Long bannedBy;

    @Column(name = "banned_by_email", length = 100)
    private String bannedByEmail;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() {
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.loginCount = 0;
        this.twoFactorEnabled = false;
        this.banType = UserBanType.NONE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.nickname = username; // 默认昵称为用户名
    }

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    /**
     * 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    /**
     * 是否为活跃状态
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isBanExpired() {
        return banExpiresAt != null && !banExpiresAt.isAfter(LocalDateTime.now());
    }

    public boolean hasActiveLoginBan() {
        return banType == UserBanType.LOGIN && !isBanExpired();
    }

    public boolean hasActiveContentBan() {
        return banType == UserBanType.CONTENT && !isBanExpired();
    }

    /**
     * 是否已启用两步验证
     */
    public boolean isTwoFactorActive() {
        return Boolean.TRUE.equals(twoFactorEnabled) && twoFactorSecret != null && !twoFactorSecret.trim().isEmpty();
    }

    /**
     * 更新登录信息
     */
    public void updateLoginInfo() {
        this.lastLoginTime = LocalDateTime.now();
        this.loginCount = (this.loginCount == null ? 0 : this.loginCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }
}
