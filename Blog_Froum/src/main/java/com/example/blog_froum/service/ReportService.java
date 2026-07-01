package com.example.blog_froum.service;

import com.example.blog_froum.dto.report.ReportCreateRequest;
import com.example.blog_froum.dto.report.ReportHandleRequest;
import com.example.blog_froum.dto.report.ReportResponse;
import com.example.blog_froum.dto.report.ReportStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ReportService {

    ReportResponse createReport(Long reporterId, ReportCreateRequest request);

    Page<ReportResponse> getUserReports(Long reporterId, String status, Pageable pageable);

    ReportResponse getUserReport(Long reporterId, Long reportId);

    Page<ReportResponse> searchReportsForAdmin(String targetType,
                                               String status,
                                               String keyword,
                                               LocalDateTime startTime,
                                               LocalDateTime endTime,
                                               Pageable pageable);

    ReportResponse getReportForAdmin(Long reportId);

    ReportResponse handleReport(Long reportId, Long handlerId, ReportHandleRequest request);

    ReportStatisticsResponse getReportStatistics();
}
