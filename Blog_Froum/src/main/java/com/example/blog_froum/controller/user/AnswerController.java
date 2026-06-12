package com.example.blog_froum.controller.user;

import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import com.example.blog_froum.dto.comment.CommentResponse;
import com.example.blog_froum.service.CommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 回答Controller
 * 处理用户回答相关的请求
 */
@RestController
@RequestMapping("/api/answers")
@Api(tags = "回答管理")
@Slf4j
public class AnswerController {

    @Autowired
    private CommentService commentService;

    /**
     * 获取指定用户的回答列表
     * 注意：这里将评论作为回答来处理，因为在当前系统中评论就是对问题的回答
     */
    @GetMapping("/user/{userId}")
    @ApiOperation(value = "获取指定用户的回答", notes = "获取指定用户发布的回答列表")
    public Result<Page<CommentResponse>> getUserAnswers(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取用户 {} 的回答列表，页码: {}, 大小: {}", userId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            
            // 获取用户的评论作为回答
            Page<CommentResponse> responsePage = commentService.getUserComments(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户回答列表失败", e);
            return Result.error("获取用户回答列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的回答列表
     */
    @GetMapping("/my")
    @ApiOperation(value = "获取我的回答", notes = "获取当前用户的回答列表")
    public Result<Page<CommentResponse>> getMyAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的回答列表", userId);
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentResponse> responsePage = commentService.getUserComments(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户回答列表失败", e);
            return Result.error("获取用户回答列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取问题的回答列表
     */
    @GetMapping("/question/{questionId}")
    @ApiOperation(value = "获取问题的回答", notes = "获取指定问题的所有回答")
    public Result<Page<CommentResponse>> getQuestionAnswers(
            @ApiParam(value = "问题ID", required = true) @PathVariable Long questionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取问题 {} 的回答列表，页码: {}, 大小: {}", questionId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            
            // 获取问题的评论作为回答
            Page<CommentResponse> responsePage = commentService.getCommentsByTarget("QUESTION", questionId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取问题回答列表失败", e);
            return Result.error("获取问题回答列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文章的回答列表
     */
    @GetMapping("/article/{articleId}")
    @ApiOperation(value = "获取文章的回答", notes = "获取指定文章的所有回答")
    public Result<Page<CommentResponse>> getArticleAnswers(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long articleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取文章 {} 的回答列表，页码: {}, 大小: {}", articleId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            
            // 获取文章的评论作为回答
            Page<CommentResponse> responsePage = commentService.getCommentsByTarget("ARTICLE", articleId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取文章回答列表失败", e);
            return Result.error("获取文章回答列表失败: " + e.getMessage());
        }
    }
}
