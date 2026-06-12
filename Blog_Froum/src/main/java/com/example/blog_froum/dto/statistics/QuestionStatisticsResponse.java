package com.example.blog_froum.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatisticsResponse {
    private long totalQuestions;
    private long approvedQuestions;
    private long pendingQuestions;
    private long rejectedQuestions;
    private long solvedQuestions;
    private long pinnedQuestions;
    private long featuredQuestions;
}
