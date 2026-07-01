package com.example.blog_froum.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatisticsResponse {

    private long totalCount;
    private long pendingCount;
    private long resolvedCount;
    private long rejectedCount;
}
