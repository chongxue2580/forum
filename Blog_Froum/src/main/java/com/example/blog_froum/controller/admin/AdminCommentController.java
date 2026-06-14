package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.comment.CommentResponse;
import com.example.blog_froum.dto.statistics.CommentStatisticsResponse;
import com.example.blog_froum.service.CommentService;
import com.example.blog_froum.service.OperationLogService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理员评论管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/comments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员评论管理")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取所有评论
     */
    @GetMapping
    @ApiOperation(value = "获取所有评论", notes = "分页获取系统中的所有评论")
    public Result<Page<CommentResponse>> getAllComments(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "目标类型") @RequestParam(required = false) String targetType,
            @ApiParam(value = "目标ID") @RequestParam(required = false) Long targetId,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
            @ApiParam(value = "删除状态：active=未删除(默认)，deleted=已删除，all=全部") @RequestParam(required = false) String status,
            @ApiParam(value = "开始日期，格式yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束日期，格式yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        try {
            log.info("管理员获取评论列表，页码: {}, 每页数量: {}, 目标类型: {}, 目标ID: {}, 关键词: {}, 状态: {}",
                    page, pageSize, targetType, targetId, keyword, status);
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));

            Page<CommentResponse> responsePage = commentService.searchCommentsForAdmin(
                    targetType,
                    targetId,
                    keyword,
                    parseStartDate(startDate),
                    parseEndDate(endDate),
                    resolveDeletedFilter(status),
                    pageable);

            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return Result.error("获取评论列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除评论", notes = "管理员删除指定评论")
    public Result<Void> deleteComment(
            @ApiParam(value = "评论ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员删除评论，ID: {}", id);
            commentService.adminDeleteComment(id);
            operationLogService.record(BaseContext.getCurrentId(), "COMMENT_MANAGEMENT", "delete",
                    "删除评论：" + id, "COMMENT", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除评论
     */
    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除评论", notes = "管理员批量删除评论")
    public Result<Void> batchDeleteComments(
            @ApiParam(value = "评论ID列表", required = true) @RequestBody java.util.List<Long> commentIds,
            HttpServletRequest request) {
        try {
            log.info("管理员批量删除评论，数量: {}", commentIds.size());
            commentService.batchDeleteComments(commentIds);
            operationLogService.record(BaseContext.getCurrentId(), "COMMENT_MANAGEMENT", "delete",
                    "批量删除评论，数量：" + commentIds.size(), "COMMENT", null, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("批量删除评论失败", e);
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 恢复评论（撤销删除）
     */
    @PutMapping("/{id}/restore")
    @ApiOperation(value = "恢复评论", notes = "管理员恢复被删除的评论")
    public Result<Void> restoreComment(
            @ApiParam(value = "评论ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员恢复评论，ID: {}", id);
            commentService.adminRestoreComment(id);
            operationLogService.record(BaseContext.getCurrentId(), "COMMENT_MANAGEMENT", "restore",
                    "恢复评论：" + id, "COMMENT", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("恢复评论失败", e);
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 获取评论统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取评论统计信息", notes = "获取评论的各种统计数据")
    public Result<CommentStatisticsResponse> getCommentStatistics() {
        try {
            log.info("管理员获取评论统计信息");
            CommentStatisticsResponse statistics = commentService.getCommentStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取评论统计信息失败", e);
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的评论列表
     */
    @GetMapping("/user/{userId}")
    @ApiOperation(value = "获取用户评论", notes = "获取指定用户的所有评论")
    public Result<Page<CommentResponse>> getUserComments(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("管理员获取用户 {} 的评论列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<CommentResponse> responsePage = commentService.getUserComments(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户评论列表失败", e);
            return Result.error("获取用户评论列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近的评论
     */
    @GetMapping("/recent")
    @ApiOperation(value = "获取最近评论", notes = "获取最近的评论列表")
    public Result<Page<CommentResponse>> getRecentComments(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "20") @RequestParam(defaultValue = "20") int pageSize) {
        try {
            log.info("管理员获取最近评论");
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<CommentResponse> responsePage = commentService.getRecentComments(pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取最近评论失败", e);
            return Result.error("获取最近评论失败: " + e.getMessage());
        }
    }

    /**
     * 将前端的 status 文本映射为删除状态过滤值。
     * active/空 -> false（仅未删除）；deleted -> true（仅已删除）；all -> null（全部）
     */
    private Boolean resolveDeletedFilter(String status) {
        if (status == null || status.trim().isEmpty() || "active".equalsIgnoreCase(status.trim())) {
            return Boolean.FALSE;
        }
        if ("deleted".equalsIgnoreCase(status.trim())) {
            return Boolean.TRUE;
        }
        return null;
    }

    private LocalDateTime parseStartDate(String value) {        if (value == null || value.trim().isEmpty()) {
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
