package com.example.blog_froum.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {
    private long totalUsers;
    private long activeUsers;
    private long disabledUsers;
    private long adminUsers;
    private long regularUsers;
}
