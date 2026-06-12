package com.example.blog_froum.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentStatisticsResponse {
    private long totalComments;
    private long articleComments;
    private long questionComments;
}
