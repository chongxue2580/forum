package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.article.ArticleResponse;
import com.example.blog_froum.dto.statistics.ArticleStatisticsResponse;
import com.example.blog_froum.service.ArticleService;
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

/**
 * 管理员文章管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/articles")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员文章管理")
public class AdminArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取所有文章
     */
    @GetMapping
    @ApiOperation(value = "获取所有文章", notes = "分页获取系统中的所有文章")
    public Result<Page<ArticleResponse>> getAllArticles(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "状态筛选") @RequestParam(required = false) String status) {
        try {
            log.info("管理员获取文章列表，页码: {}, 每页数量: {}, 状态: {}", page, pageSize, status);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            
            Page<ArticleResponse> responsePage;
            if (status != null && !status.isEmpty()) {
                responsePage = articleService.getArticlesByStatus(status, pageable);
            } else {
                responsePage = articleService.getAllArticles(pageable);
            }
            
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取文章列表失败", e);
            return Result.error("获取文章列表失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过文章
     */
    @PostMapping("/{id}/approve")
    @ApiOperation(value = "审核通过文章", notes = "管理员审核通过指定文章")
    public Result<Void> approveArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员审核通过文章，ID: {}", id);
            articleService.approveArticle(id);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "approve",
                    "审核通过文章：" + id, "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核通过文章失败", e);
            return Result.error("审核通过失败: " + e.getMessage());
        }
    }

    /**
     * 审核拒绝文章
     */
    @PostMapping("/{id}/reject")
    @ApiOperation(value = "审核拒绝文章", notes = "管理员审核拒绝指定文章")
    public Result<Void> rejectArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            @ApiParam(value = "拒绝原因") @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        try {
            log.info("管理员审核拒绝文章，ID: {}, 原因: {}", id, reason);
            articleService.rejectArticle(id, reason);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "reject",
                    "审核拒绝文章：" + id + (reason == null ? "" : "，原因：" + reason), "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核拒绝文章失败", e);
            return Result.error("审核拒绝失败: " + e.getMessage());
        }
    }

    /**
     * 设置文章为置顶
     */
    @PostMapping("/{id}/pin")
    @ApiOperation(value = "设置文章为置顶", notes = "管理员设置文章为置顶")
    public Result<Void> pinArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员设置文章置顶，ID: {}", id);
            articleService.pinArticle(id);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "pin",
                    "置顶文章：" + id, "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("设置文章置顶失败", e);
            return Result.error("设置置顶失败: " + e.getMessage());
        }
    }

    /**
     * 取消文章置顶
     */
    @PostMapping("/{id}/unpin")
    @ApiOperation(value = "取消文章置顶", notes = "管理员取消文章置顶")
    public Result<Void> unpinArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员取消文章置顶，ID: {}", id);
            articleService.unpinArticle(id);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "unpin",
                    "取消置顶文章：" + id, "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("取消文章置顶失败", e);
            return Result.error("取消置顶失败: " + e.getMessage());
        }
    }

    /**
     * 设置文章为精选
     */
    @PostMapping("/{id}/feature")
    @ApiOperation(value = "设置文章为精选", notes = "管理员设置文章为精选")
    public Result<Void> featureArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员设置文章精选，ID: {}", id);
            articleService.featureArticle(id);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "feature",
                    "设置精选文章：" + id, "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("设置文章精选失败", e);
            return Result.error("设置精选失败: " + e.getMessage());
        }
    }

    /**
     * 取消文章精选
     */
    @PostMapping("/{id}/unfeature")
    @ApiOperation(value = "取消文章精选", notes = "管理员取消文章精选")
    public Result<Void> unfeatureArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员取消文章精选，ID: {}", id);
            articleService.unfeatureArticle(id);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "unfeature",
                    "取消精选文章：" + id, "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("取消文章精选失败", e);
            return Result.error("取消精选失败: " + e.getMessage());
        }
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除文章", notes = "管理员删除指定文章")
    public Result<Void> deleteArticle(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员删除文章，ID: {}", id);
            articleService.adminDeleteArticle(id);
            operationLogService.record(BaseContext.getCurrentId(), "ARTICLE_MANAGEMENT", "delete",
                    "删除文章：" + id, "ARTICLE", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("删除文章失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文章统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取文章统计信息", notes = "获取文章的各种统计数据")
    public Result<ArticleStatisticsResponse> getArticleStatistics() {
        try {
            log.info("管理员获取文章统计信息");
            ArticleStatisticsResponse statistics = articleService.getArticleStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取文章统计信息失败", e);
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }
}
