package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.question.QuestionResponse;
import com.example.blog_froum.dto.statistics.QuestionStatisticsResponse;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.QuestionService;
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
import java.util.Locale;

/**
 * 管理员问答管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/questions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员问答管理")
public class AdminQuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取所有问答
     */
    @GetMapping
    @ApiOperation(value = "获取所有问答", notes = "分页获取系统中的所有问答")
    public Result<Page<QuestionResponse>> getAllQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "状态筛选") @RequestParam(required = false) String status) {
        try {
            log.info("管理员获取问答列表，页码: {}, 每页数量: {}, 状态: {}", page, pageSize, status);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            
            Page<QuestionResponse> responsePage;
            if (status != null && !status.isEmpty()) {
                responsePage = questionService.getQuestionsByStatus(status.trim().toUpperCase(Locale.ROOT), pageable);
            } else {
                responsePage = questionService.getQuestions(pageable);
            }
            
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取问答列表失败", e);
            return Result.error("获取问答列表失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过问答
     */
    @PostMapping("/{id}/approve")
    @ApiOperation(value = "审核通过问答", notes = "管理员审核通过指定问答")
    public Result<Void> approveQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员审核通过问答，ID: {}", id);
            questionService.approveQuestion(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "approve",
                    "审核通过问答：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核通过问答失败", e);
            return Result.error("审核通过失败: " + e.getMessage());
        }
    }

    /**
     * 审核拒绝问答
     */
    @PostMapping("/{id}/reject")
    @ApiOperation(value = "审核拒绝问答", notes = "管理员审核拒绝指定问答")
    public Result<Void> rejectQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            @ApiParam(value = "拒绝原因") @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        try {
            log.info("管理员审核拒绝问答，ID: {}, 原因: {}", id, reason);
            questionService.rejectQuestion(id, reason);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "reject",
                    "审核拒绝问答：" + id + (reason == null ? "" : "，原因：" + reason), "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核拒绝问答失败", e);
            return Result.error("审核拒绝失败: " + e.getMessage());
        }
    }

    /**
     * 设置问答为置顶
     */
    @PostMapping("/{id}/pin")
    @ApiOperation(value = "设置问答为置顶", notes = "管理员设置问答为置顶")
    public Result<Void> pinQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员设置问答置顶，ID: {}", id);
            questionService.pinQuestion(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "pin",
                    "置顶问答：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("设置问答置顶失败", e);
            return Result.error("设置置顶失败: " + e.getMessage());
        }
    }

    /**
     * 取消问答置顶
     */
    @PostMapping("/{id}/unpin")
    @ApiOperation(value = "取消问答置顶", notes = "管理员取消问答置顶")
    public Result<Void> unpinQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员取消问答置顶，ID: {}", id);
            questionService.unpinQuestion(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "unpin",
                    "取消置顶问答：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("取消问答置顶失败", e);
            return Result.error("取消置顶失败: " + e.getMessage());
        }
    }

    /**
     * 设置问答为精选
     */
    @PostMapping("/{id}/feature")
    @ApiOperation(value = "设置问答为精选", notes = "管理员设置问答为精选")
    public Result<Void> featureQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员设置问答精选，ID: {}", id);
            questionService.featureQuestion(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "feature",
                    "设置精选问答：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("设置问答精选失败", e);
            return Result.error("设置精选失败: " + e.getMessage());
        }
    }

    /**
     * 取消问答精选
     */
    @PostMapping("/{id}/unfeature")
    @ApiOperation(value = "取消问答精选", notes = "管理员取消问答精选")
    public Result<Void> unfeatureQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员取消问答精选，ID: {}", id);
            questionService.unfeatureQuestion(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "unfeature",
                    "取消精选问答：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("取消问答精选失败", e);
            return Result.error("取消精选失败: " + e.getMessage());
        }
    }

    /**
     * 标记问答为已解决
     */
    @PostMapping("/{id}/solve")
    @ApiOperation(value = "标记问答为已解决", notes = "管理员标记指定问答为已解决")
    public Result<Void> solveQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员标记问答为已解决，ID: {}", id);
            questionService.adminMarkQuestionAsSolved(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "solve",
                    "标记问答已解决：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("标记问答为已解决失败", e);
            return Result.error("标记已解决失败: " + e.getMessage());
        }
    }

    /**
     * 标记问答为未解决
     */
    @PostMapping("/{id}/unsolve")
    @ApiOperation(value = "标记问答为未解决", notes = "管理员标记指定问答为未解决")
    public Result<Void> unsolveQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员标记问答为未解决，ID: {}", id);
            questionService.adminMarkQuestionAsUnsolved(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "unsolve",
                    "标记问答未解决：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("标记问答为未解决失败", e);
            return Result.error("标记未解决失败: " + e.getMessage());
        }
    }

    /**
     * 删除问答
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除问答", notes = "管理员删除指定问答")
    public Result<Void> deleteQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员删除问答，ID: {}", id);
            questionService.adminDeleteQuestion(id);
            operationLogService.record(BaseContext.getCurrentId(), "QUESTION_MANAGEMENT", "delete",
                    "删除问答：" + id, "QUESTION", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("删除问答失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取问答统计信息", notes = "获取问答的各种统计数据")
    public Result<QuestionStatisticsResponse> getQuestionStatistics() {
        try {
            log.info("管理员获取问答统计信息");
            QuestionStatisticsResponse statistics = questionService.getQuestionStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取问答统计信息失败", e);
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }
}
