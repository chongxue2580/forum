package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.admin.OperationLogResponse;
import com.example.blog_froum.service.OperationLogService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin/operation-logs")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员操作日志")
public class AdminOperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    @ApiOperation(value = "获取操作日志", notes = "分页查询管理员和系统操作日志")
    public Result<Page<OperationLogResponse>> getOperationLogs(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "20") @RequestParam(defaultValue = "20") int pageSize,
            @ApiParam(value = "操作类型") @RequestParam(required = false) String type,
            @ApiParam(value = "操作动作") @RequestParam(required = false) String action,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
            @ApiParam(value = "开始日期，格式yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束日期，格式yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        try {
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
            LocalDateTime startTime = parseStartDate(startDate);
            LocalDateTime endTime = parseEndDate(endDate);
            Page<OperationLogResponse> logs = operationLogService.searchLogs(type, action, keyword, startTime, endTime, pageable);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("获取操作日志失败", e);
            return Result.error("获取操作日志失败: " + e.getMessage());
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
