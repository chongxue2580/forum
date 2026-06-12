package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.like.LikeInfoResponse;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import com.example.blog_froum.service.LikeService;
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
 * 点赞控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/likes")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "点赞管理")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /**
     * 点赞目标
     */
    @PostMapping("/{targetType}/{targetId}")
    @ApiOperation(value = "点赞目标", notes = "用户点赞指定类型的目标")
    public Result<Void> likeTarget(
            @ApiParam(value = "目标类型", required = true, example = "ARTICLE") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 点赞 {} {}", userId, targetType, targetId);
            likeService.likeTarget(userId, targetType.toUpperCase(), targetId);
            return Result.success("点赞成功");
        } catch (Exception e) {
            log.error("点赞失败", e);
            return Result.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞目标
     */
    @DeleteMapping("/{targetType}/{targetId}")
    @ApiOperation(value = "取消点赞目标", notes = "用户取消点赞指定类型的目标")
    public Result<Void> unlikeTarget(
            @ApiParam(value = "目标类型", required = true, example = "ARTICLE") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 取消点赞 {} {}", userId, targetType, targetId);
            likeService.unlikeTarget(userId, targetType.toUpperCase(), targetId);
            return Result.success("取消点赞成功");
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return Result.error("取消点赞失败: " + e.getMessage());
        }
    }

    /**
     * 检查点赞状态
     */
    @GetMapping("/{targetType}/{targetId}/status")
    @ApiOperation(value = "检查点赞状态", notes = "检查用户是否已点赞指定目标")
    public Result<Boolean> checkLikeStatus(
            @ApiParam(value = "目标类型", required = true, example = "ARTICLE") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.success(false);
            }

            boolean isLiked = likeService.isTargetLikedByUser(userId, targetType.toUpperCase(), targetId);
            return Result.success(isLiked);
        } catch (Exception e) {
            log.error("检查点赞状态失败", e);
            return Result.error("检查点赞状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取目标点赞数
     */
    @GetMapping("/{targetType}/{targetId}/count")
    @ApiOperation(value = "获取点赞数", notes = "获取指定目标的点赞数量")
    public Result<Long> getLikeCount(
            @ApiParam(value = "目标类型", required = true, example = "ARTICLE") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            long count = likeService.getTargetLikeCount(targetType.toUpperCase(), targetId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取点赞数失败", e);
            return Result.error("获取点赞数失败: " + e.getMessage());
        }
    }

    /**
     * 获取目标点赞信息（包含状态和数量）
     */
    @GetMapping("/{targetType}/{targetId}/info")
    @ApiOperation(value = "获取点赞信息", notes = "获取指定目标的点赞状态和数量")
    public Result<LikeInfoResponse> getLikeInfo(
            @ApiParam(value = "目标类型", required = true, example = "ARTICLE") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();

            String normalizedTargetType = targetType.toUpperCase();
            LikeInfoResponse info = new LikeInfoResponse(
                    likeService.getTargetLikeCount(normalizedTargetType, targetId),
                    userId != null && likeService.isTargetLikedByUser(userId, normalizedTargetType, targetId));
            
            return Result.success(info);
        } catch (Exception e) {
            log.error("获取点赞信息失败", e);
            return Result.error("获取点赞信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户点赞的文章列表
     */
    @GetMapping("/articles")
    @ApiOperation(value = "获取用户点赞的文章", notes = "获取当前用户点赞的文章列表")
    public Result<Page<Long>> getUserLikedArticles(
            @ApiParam(value = "页码", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Long> articles = likeService.getUserLikedArticles(userId, pageable);
            return Result.success(articles);
        } catch (Exception e) {
            log.error("获取用户点赞文章失败", e);
            return Result.error("获取用户点赞文章失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户点赞的问答列表
     */
    @GetMapping("/questions")
    @ApiOperation(value = "获取用户点赞的问答", notes = "获取当前用户点赞的问答列表")
    public Result<Page<Long>> getUserLikedQuestions(
            @ApiParam(value = "页码", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Long> questions = likeService.getUserLikedQuestions(userId, pageable);
            return Result.success(questions);
        } catch (Exception e) {
            log.error("获取用户点赞问答失败", e);
            return Result.error("获取用户点赞问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户点赞的评论列表
     */
    @GetMapping("/comments")
    @ApiOperation(value = "获取用户点赞的评论", notes = "获取当前用户点赞的评论列表")
    public Result<Page<Long>> getUserLikedComments(
            @ApiParam(value = "页码", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Long> comments = likeService.getUserLikedComments(userId, pageable);
            return Result.success(comments);
        } catch (Exception e) {
            log.error("获取用户点赞评论失败", e);
            return Result.error("获取用户点赞评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户总点赞数
     */
    @GetMapping("/user/total")
    @ApiOperation(value = "获取用户总点赞数", notes = "获取当前用户的总点赞数")
    public Result<Long> getUserTotalLikeCount() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            long count = likeService.getUserTotalLikeCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取用户总点赞数失败", e);
            return Result.error("获取用户总点赞数失败: " + e.getMessage());
        }
    }
}
