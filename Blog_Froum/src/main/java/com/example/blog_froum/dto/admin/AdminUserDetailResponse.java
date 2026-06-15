package com.example.blog_froum.dto.admin;

import com.example.blog_froum.entity.OperationLog;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserBanType;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 管理员用户详情响应，不包含密码和二次验证密钥。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
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

    private UserBanType banType;
    private Boolean banned;
    private String banReason;
    private LocalDateTime banExpiresAt;
    private LocalDateTime bannedAt;
    private Long bannedBy;
    private String bannedByEmail;

    private String lastKnownIp;
    private String lastKnownUserAgent;
    private LocalDateTime lastAccessTime;
    private Boolean online;

    private Long articleCount;
    private Long questionCount;
    private Long commentCount;
    private Long likeCount;
    private Long followingCount;
    private Long followerCount;

    public static AdminUserDetailResponse from(User user,
                                               OperationLog latestAccessLog,
                                               long articleCount,
                                               long questionCount,
                                               long commentCount,
                                               long likeCount,
                                               long followingCount,
                                               long followerCount) {
        User source = Objects.requireNonNull(user, "user");
        UserBanType banType = source.getBanType() == null
                ? (source.getStatus() == UserStatus.ACTIVE ? UserBanType.NONE : UserBanType.LOGIN)
                : source.getBanType();
        boolean banned = banType != UserBanType.NONE && !source.isBanExpired();
        LocalDateTime lastAccessTime = latestAccessLog == null ? null : latestAccessLog.getCreatedAt();

        return AdminUserDetailResponse.builder()
                .id(source.getId())
                .username(source.getUsername())
                .email(source.getEmail())
                .nickname(source.getNickname())
                .bio(source.getBio())
                .avatarUrl(source.getAvatarUrl())
                .role(source.getRole())
                .status(source.getStatus())
                .lastLoginTime(source.getLastLoginTime())
                .loginCount(source.getLoginCount())
                .twoFactorEnabled(Boolean.TRUE.equals(source.getTwoFactorEnabled()))
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .banType(banType)
                .banned(banned)
                .banReason(source.getBanReason())
                .banExpiresAt(source.getBanExpiresAt())
                .bannedAt(source.getBannedAt())
                .bannedBy(source.getBannedBy())
                .bannedByEmail(source.getBannedByEmail())
                .lastKnownIp(latestAccessLog == null ? null : latestAccessLog.getIpAddress())
                .lastKnownUserAgent(latestAccessLog == null ? null : latestAccessLog.getUserAgent())
                .lastAccessTime(lastAccessTime)
                .online(lastAccessTime != null && lastAccessTime.isAfter(LocalDateTime.now().minusMinutes(15)))
                .articleCount(articleCount)
                .questionCount(questionCount)
                .commentCount(commentCount)
                .likeCount(likeCount)
                .followingCount(followingCount)
                .followerCount(followerCount)
                .build();
    }
}
