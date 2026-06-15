package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.follow.FollowInfoResponse;
import com.example.blog_froum.dto.follow.FollowQuestionResponse;
import com.example.blog_froum.dto.follow.FollowStatsResponse;
import com.example.blog_froum.dto.follow.FollowUserResponse;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import com.example.blog_froum.service.FollowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/follows")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "关注管理")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 关注目标
     */
    @PostMapping("/{targetType}/{targetId}")
    @ApiOperation(value = "关注目标", notes = "用户关注指定类型的目标")
    public Result<Void> followTarget(
            @ApiParam(value = "目标类型", required = true, example = "USER") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 关注 {} {}", userId, targetType, targetId);
            followService.followTarget(userId, targetType.toUpperCase(), targetId);
            return Result.success("关注成功");
        } catch (Exception e) {
            log.error("关注失败", e);
            return Result.error("关注失败: " + e.getMessage());
        }
    }

    /**
     * 取消关注目标
     */
    @DeleteMapping("/{targetType}/{targetId}")
    @ApiOperation(value = "取消关注目标", notes = "用户取消关注指定类型的目标")
    public Result<Void> unfollowTarget(
            @ApiParam(value = "目标类型", required = true, example = "USER") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 取消关注 {} {}", userId, targetType, targetId);
            followService.unfollowTarget(userId, targetType.toUpperCase(), targetId);
            return Result.success("取消关注成功");
        } catch (Exception e) {
            log.error("取消关注失败", e);
            return Result.error("取消关注失败: " + e.getMessage());
        }
    }

    /**
     * 检查关注状态
     */
    @GetMapping("/{targetType}/{targetId}/status")
    @ApiOperation(value = "检查关注状态", notes = "检查用户是否已关注指定目标")
    public Result<Boolean> checkFollowStatus(
            @ApiParam(value = "目标类型", required = true, example = "USER") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.success(false);
            }

            boolean isFollowed = followService.isTargetFollowedByUser(userId, targetType.toUpperCase(), targetId);
            return Result.success(isFollowed);
        } catch (Exception e) {
            log.error("检查关注状态失败", e);
            return Result.error("检查关注状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取目标关注数
     */
    @GetMapping("/{targetType}/{targetId}/count")
    @ApiOperation(value = "获取关注数", notes = "获取指定目标的关注数量")
    public Result<Long> getFollowCount(
            @ApiParam(value = "目标类型", required = true, example = "USER") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            long count = followService.getTargetFollowCount(targetType.toUpperCase(), targetId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取关注数失败", e);
            return Result.error("获取关注数失败: " + e.getMessage());
        }
    }

    /**
     * 获取目标关注信息（包含状态和数量）
     */
    @GetMapping("/{targetType}/{targetId}/info")
    @ApiOperation(value = "获取关注信息", notes = "获取指定目标的关注状态和数量")
    public Result<FollowInfoResponse> getFollowInfo(
            @ApiParam(value = "目标类型", required = true, example = "USER") @PathVariable String targetType,
            @ApiParam(value = "目标ID", required = true, example = "1") @PathVariable Long targetId) {
        try {
            Long userId = BaseContext.getCurrentId();

            String normalizedTargetType = targetType.toUpperCase();
            FollowInfoResponse info = new FollowInfoResponse(
                    followService.getTargetFollowCount(normalizedTargetType, targetId),
                    userId != null && followService.isTargetFollowedByUser(userId, normalizedTargetType, targetId));
            
            return Result.success(info);
        } catch (Exception e) {
            log.error("获取关注信息失败", e);
            return Result.error("获取关注信息失败: " + e.getMessage());
        }
    }


    /**
     * 兼容旧用户关注接口：关注用户
     */
    @PostMapping("/users/{targetUserId}")
    @ApiOperation(value = "关注用户", notes = "兼容旧接口，实际写入统一 follows 表")
    public Result<Void> followUser(
            @ApiParam(value = "旧接口用户ID参数，优先使用当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "目标用户ID", required = true, example = "2")
            @PathVariable Long targetUserId) {
        try {
            Long currentUserId = resolveUserId(userId);
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            followService.followTarget(currentUserId, "USER", targetUserId);
            return Result.success("关注成功");
        } catch (Exception e) {
            log.error("关注用户失败", e);
            return Result.error("关注用户失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧用户关注接口：取消关注用户
     */
    @DeleteMapping("/users/{targetUserId}")
    @ApiOperation(value = "取消关注用户", notes = "兼容旧接口，实际删除统一 follows 表记录")
    public Result<Void> unfollowUser(
            @ApiParam(value = "旧接口用户ID参数，优先使用当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "目标用户ID", required = true, example = "2")
            @PathVariable Long targetUserId) {
        try {
            Long currentUserId = resolveUserId(userId);
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            followService.unfollowTarget(currentUserId, "USER", targetUserId);
            return Result.success("取消关注成功");
        } catch (Exception e) {
            log.error("取消关注用户失败", e);
            return Result.error("取消关注用户失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧用户关注接口：检查是否关注用户
     */
    @GetMapping("/users/check")
    @ApiOperation(value = "检查用户关注状态", notes = "兼容旧接口，实际读取统一 follows 表")
    public Result<Boolean> checkUserFollowing(
            @ApiParam(value = "旧接口用户ID参数，优先使用当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "目标用户ID", required = true, example = "2")
            @RequestParam Long targetUserId) {
        try {
            Long currentUserId = resolveUserId(userId);
            if (currentUserId == null) {
                return Result.success(false);
            }

            boolean isFollowing = followService.isTargetFollowedByUser(currentUserId, "USER", targetUserId);
            return Result.success(isFollowing);
        } catch (Exception e) {
            log.error("检查用户关注状态失败", e);
            return Result.error("检查用户关注状态失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧用户关注接口：获取关注列表
     */
    @GetMapping("/users/following")
    @ApiOperation(value = "获取用户关注列表", notes = "兼容旧接口，返回统一 follows 表中的用户关注列表")
    public Result<List<FollowUserResponse>> getFollowing(
            @ApiParam(value = "用户ID，不传则查询当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long queryUserId = resolveUserId(userId);
            if (queryUserId == null) {
                return Result.error("用户未登录");
            }

            return Result.success(followService.getUserFollowedUsers(queryUserId, page, size).getContent());
        } catch (Exception e) {
            log.error("获取用户关注列表失败", e);
            return Result.error("获取用户关注列表失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧用户关注接口：获取粉丝列表
     */
    @GetMapping("/users/followers")
    @ApiOperation(value = "获取用户粉丝列表", notes = "兼容旧接口，返回统一 follows 表中的用户粉丝列表")
    public Result<List<FollowUserResponse>> getUserFollowersCompat(
            @ApiParam(value = "用户ID，不传则查询当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long queryUserId = resolveUserId(userId);
            if (queryUserId == null) {
                return Result.error("用户未登录");
            }

            return Result.success(followService.getUserFollowers(queryUserId, page, size).getContent());
        } catch (Exception e) {
            log.error("获取用户粉丝列表失败", e);
            return Result.error("获取用户粉丝列表失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧用户关注接口：获取关注数量
     */
    @GetMapping("/users/{userId}/following/count")
    @ApiOperation(value = "获取用户关注数量", notes = "兼容旧接口，统计统一 follows 表中的 USER 关注数量")
    public Result<Long> getFollowingCount(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            return Result.success(followService.getUserFollowingCount(userId, "USER"));
        } catch (Exception e) {
            log.error("获取用户关注数量失败", e);
            return Result.error("获取用户关注数量失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧用户关注接口：获取粉丝数量
     */
    @GetMapping("/users/{userId}/followers/count")
    @ApiOperation(value = "获取用户粉丝数量", notes = "兼容旧接口，统计统一 follows 表中的 USER 粉丝数量")
    public Result<Long> getFollowersCount(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            return Result.success(followService.getUserFollowerCount(userId));
        } catch (Exception e) {
            log.error("获取用户粉丝数量失败", e);
            return Result.error("获取用户粉丝数量失败: " + e.getMessage());
        }
    }






    /**
     * 获取用户关注数量统计
     */
    @GetMapping("/stats")
    @ApiOperation(value = "获取关注统计", notes = "获取当前用户的关注数量统计")
    public Result<FollowStatsResponse> getFollowStats() {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            FollowStatsResponse stats = new FollowStatsResponse(
                    followService.getUserFollowingCount(userId, "USER"),
                    followService.getUserFollowingCount(userId, "QUESTION"),
                    followService.getTargetFollowCount("USER", userId));

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取关注统计失败", e);
            return Result.error("获取关注统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户关注的用户列表
     */
    @GetMapping("/users")
    @ApiOperation(value = "获取关注的用户列表", notes = "获取当前用户关注的用户列表")
    public Result<Page<FollowUserResponse>> getFollowedUsers(
            @ApiParam(value = "用户ID，不传则查询当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long queryUserId = userId != null ? userId : BaseContext.getCurrentId();
            if (queryUserId == null) {
                return Result.error("用户未登录");
            }

            Page<FollowUserResponse> followedUsers = followService.getUserFollowedUsers(queryUserId, page, size);
            return Result.success(followedUsers);
        } catch (Exception e) {
            log.error("获取关注用户列表失败", e);
            return Result.error("获取关注用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的粉丝列表
     */
    @GetMapping("/followers")
    @ApiOperation(value = "获取粉丝列表", notes = "获取当前用户的粉丝列表")
    public Result<Page<FollowUserResponse>> getFollowers(
            @ApiParam(value = "用户ID，不传则查询当前登录用户")
            @RequestParam(required = false) Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long queryUserId = userId != null ? userId : BaseContext.getCurrentId();
            if (queryUserId == null) {
                return Result.error("用户未登录");
            }

            Page<FollowUserResponse> followers = followService.getUserFollowers(queryUserId, page, size);
            return Result.success(followers);
        } catch (Exception e) {
            log.error("获取粉丝列表失败", e);
            return Result.error("获取粉丝列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户关注的问题列表
     */
    @GetMapping("/questions")
    @ApiOperation(value = "获取关注的问题列表", notes = "获取当前用户关注的问题列表")
    public Result<Page<FollowQuestionResponse>> getFollowedQuestions(
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            Page<FollowQuestionResponse> followedQuestions = followService.getUserFollowedQuestions(userId, page, size);
            return Result.success(followedQuestions);
        } catch (Exception e) {
            log.error("获取关注问题列表失败", e);
            return Result.error("获取关注问题列表失败: " + e.getMessage());
        }
    }

    private Long resolveUserId(Long requestUserId) {
        Long currentUserId = BaseContext.getCurrentId();
        return currentUserId != null ? currentUserId : requestUserId;
    }
}
