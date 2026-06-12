package com.example.blog_froum.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String bio;
    private LocalDateTime followedAt;
}
