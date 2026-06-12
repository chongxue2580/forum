package com.example.blog_froum.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatisticsResponse {
    private long totalArticles;
    private long approvedArticles;
    private long pendingArticles;
    private long rejectedArticles;
    private long pinnedArticles;
    private long featuredArticles;
}
