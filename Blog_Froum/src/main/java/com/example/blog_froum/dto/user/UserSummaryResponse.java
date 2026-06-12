package com.example.blog_froum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String nickname;
    private String bio;
    private String avatarUrl;
    private LocalDateTime createdAt;

    public static UserSummaryResponse fromUserResponse(UserResponse user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt());
    }
}
