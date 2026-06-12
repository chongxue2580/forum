package com.example.blog_froum.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatsResponse {
    private long followingUsers;
    private long followingQuestions;
    private long followers;
}
