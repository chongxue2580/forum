package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.report.ReportCreateRequest;
import com.example.blog_froum.dto.report.ReportResponse;
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

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "举报管理")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    @ApiOperation(value = "提交举报", notes = "用户举报文章、问答、评论或用户")
    public Result<ReportResponse> createReport(@Valid @RequestBody ReportCreateRequest request) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 提交举报", userId);
            ReportResponse response = reportService.createReport(userId, request);
            return Result.success("举报提交成功", response);
        } catch (Exception e) {
            log.error("提交举报失败", e);
            return Result.error("提交举报失败: " + e.getMessage());
        }
    }

    @GetMapping("/my")
    @ApiOperation(value = "获取我的举报", notes = "分页获取当前用户提交的举报记录")
    public Result<Page<ReportResponse>> getMyReports(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "状态：PENDING, RESOLVED, REJECTED") @RequestParam(required = false) String status) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
            return Result.success(reportService.getUserReports(userId, status, pageable));
        } catch (Exception e) {
            log.error("获取我的举报失败", e);
            return Result.error("获取我的举报失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取我的举报详情", notes = "获取当前用户提交的单条举报详情")
    public Result<ReportResponse> getMyReport(
            @ApiParam(value = "举报ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            return Result.success(reportService.getUserReport(userId, id));
        } catch (Exception e) {
            log.error("获取举报详情失败", e);
            return Result.error("获取举报详情失败: " + e.getMessage());
        }
    }
}
