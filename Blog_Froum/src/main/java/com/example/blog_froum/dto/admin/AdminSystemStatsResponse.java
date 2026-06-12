package com.example.blog_froum.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemStatsResponse {
    private int totalUsers;
    private int totalArticles;
    private int totalComments;
    private int onlineUsers;
}
