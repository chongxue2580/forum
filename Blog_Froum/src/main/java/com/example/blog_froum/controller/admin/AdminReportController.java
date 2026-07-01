package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.report.ReportHandleRequest;
import com.example.blog_froum.dto.report.ReportResponse;
import com.example.blog_froum.dto.report.ReportStatisticsResponse;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.ReportService;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "管理员举报管理")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    @ApiOperation(value = "获取举报列表", notes = "分页筛选举报记录")
    public Result<Page<ReportResponse>> getReports(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "目标类型：ARTICLE, QUESTION, COMMENT, USER") @RequestParam(required = false) String targetType,
            @ApiParam(value = "状态：PENDING, RESOLVED, REJECTED") @RequestParam(required = false) String status,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
            @ApiParam(value = "开始日期，格式yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束日期，格式yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        try {
            log.info("管理员获取举报列表，页码: {}, 每页数量: {}, 目标类型: {}, 状态: {}, 关键词: {}",
                    page, pageSize, targetType, status, keyword);
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
            Page<ReportResponse> responsePage = reportService.searchReportsForAdmin(
                    targetType,
                    status,
                    keyword,
                    parseStartDate(startDate),
                    parseEndDate(endDate),
                    pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取举报列表失败", e);
            return Result.error("获取举报列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @ApiOperation(value = "获取举报统计", notes = "获取举报总数和各状态数量")
    public Result<ReportStatisticsResponse> getReportStatistics() {
        try {
            return Result.success(reportService.getReportStatistics());
        } catch (Exception e) {
            log.error("获取举报统计失败", e);
            return Result.error("获取举报统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取举报详情", notes = "管理员查看举报详情")
    public Result<ReportResponse> getReport(
            @ApiParam(value = "举报ID", required = true) @PathVariable Long id) {
        try {
            return Result.success(reportService.getReportForAdmin(id));
        } catch (Exception e) {
            log.error("获取举报详情失败", e);
            return Result.error("获取举报详情失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/handle")
    @ApiOperation(value = "处理举报", notes = "管理员将举报处理为已处理或驳回")
    public Result<ReportResponse> handleReport(
            @ApiParam(value = "举报ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ReportHandleRequest handleRequest,
            HttpServletRequest request) {
        try {
            Long handlerId = BaseContext.getCurrentId();
            ReportResponse response = reportService.handleReport(id, handlerId, handleRequest);
            operationLogService.record(handlerId, "REPORT_MANAGEMENT", "handle",
                    "处理举报：" + id + " -> " + response.getStatus(),
                    "REPORT", id, response.getTargetTitle(), request);
            return Result.success("举报处理成功", response);
        } catch (Exception e) {
            log.error("处理举报失败", e);
            return Result.error("处理举报失败: " + e.getMessage());
        }
    }

    private LocalDateTime parseStartDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay();
    }
}
