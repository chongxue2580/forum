package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.comment.CommentCreateRequest;
import com.example.blog_froum.dto.comment.CommentResponse;
import com.example.blog_froum.service.CommentService;
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

/**
 * 评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "评论管理")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 创建评论
     */
    @PostMapping
    @ApiOperation(value = "创建评论", notes = "用户创建评论或回复评论")
    public Result<CommentResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            log.info("用户 {} 创建评论", userId);
            CommentResponse response = commentService.createComment(userId, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("创建评论失败", e);
            return Result.error("创建评论失败: " + e.getMessage());
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除评论", notes = "用户删除自己的评论")
    public Result<Void> deleteComment(
            @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            log.info("用户 {} 删除评论 {}", userId, id);
            commentService.deleteComment(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return Result.error("删除评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取单条评论详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取评论详情", notes = "根据评论ID获取单条未删除评论")
    public Result<CommentResponse> getCommentById(
            @ApiParam(value = "评论ID", required = true) @PathVariable Long id) {
        try {
            log.info("获取评论详情 {}", id);
            CommentResponse response = commentService.getCommentById(id);
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取评论详情失败", e);
            return Result.error("获取评论详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取文章的评论列表
     */
    @GetMapping("/article/{articleId}")
    @ApiOperation(value = "获取文章评论", notes = "分页获取文章的评论列表")
    public Result<Page<CommentResponse>> getArticleComments(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long articleId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取文章 {} 的评论列表", articleId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<CommentResponse> responsePage = commentService.getArticleComments(articleId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取文章评论失败", e);
            return Result.error("获取文章评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答的评论列表
     */
    @GetMapping("/question/{questionId}")
    @ApiOperation(value = "获取问答评论", notes = "分页获取问答的评论列表")
    public Result<Page<CommentResponse>> getQuestionComments(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long questionId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取问答 {} 的评论列表", questionId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<CommentResponse> responsePage = commentService.getQuestionComments(questionId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取问答评论失败", e);
            return Result.error("获取问答评论失败: " + e.getMessage());
        }
    }

    /**
     * 通用方法：获取目标的评论列表
     */
    @GetMapping("/{targetType}/{targetId}")
    @ApiOperation(value = "获取目标评论", notes = "分页获取指定目标的评论列表")
    public Result<Page<CommentResponse>> getTargetComments(
            @ApiParam(value = "目标类型", required = true, allowableValues = "ARTICLE,QUESTION") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true) @PathVariable Long targetId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取 {} {} 的评论列表", targetType, targetId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<CommentResponse> responsePage = commentService.getTargetComments(targetType, targetId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取目标评论失败", e);
            return Result.error("获取目标评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的评论列表
     */
    @GetMapping("/my")
    @ApiOperation(value = "获取我的评论", notes = "获取当前用户的评论列表")
    public Result<Page<CommentResponse>> getMyComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            log.info("获取用户 {} 的评论列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<CommentResponse> responsePage = commentService.getUserComments(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户评论列表失败", e);
            return Result.error("获取用户评论列表失败: " + e.getMessage());
        }
    }

    /**
     * 点赞评论
     */
    @PostMapping("/{id}/like")
    @ApiOperation(value = "点赞评论", notes = "用户点赞评论")
    public Result<Void> likeComment(
            @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            log.info("用户 {} 点赞评论 {}", userId, id);
            commentService.likeComment(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("点赞评论失败", e);
            return Result.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/{id}/like")
    @ApiOperation(value = "取消点赞评论", notes = "用户取消点赞评论")
    public Result<Void> unlikeComment(
            @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            log.info("用户 {} 取消点赞评论 {}", userId, id);
            commentService.unlikeComment(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消点赞评论失败", e);
            return Result.error("取消点赞失败: " + e.getMessage());
        }
    }
}
