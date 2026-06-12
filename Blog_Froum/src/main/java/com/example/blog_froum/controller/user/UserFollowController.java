package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.service.UserFollowService;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户关注控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/follows/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "用户关注管理")
public class UserFollowController {

    @Autowired
    private UserFollowService userFollowService;

    /**
     * 关注用户
     */
    @PostMapping("/{targetUserId}")
    @ApiOperation(value = "关注用户", notes = "当前用户关注目标用户")
    public Result<Void> followUser(
            @ApiParam(value = "当前用户ID", required = true, example = "1")
            @RequestParam Long userId,
            @ApiParam(value = "目标用户ID", required = true, example = "2")
            @PathVariable Long targetUserId) {
        try {
            userFollowService.followUser(userId, targetUserId);
            return Result.success("关注成功");
        } catch (Exception e) {
            log.error("关注用户失败，用户ID: {}，目标用户ID: {}，错误: {}", userId, targetUserId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消关注用户
     */
    @DeleteMapping("/{targetUserId}")
    @ApiOperation(value = "取消关注", notes = "当前用户取消关注目标用户")
    public Result<Void> unfollowUser(
            @ApiParam(value = "当前用户ID", required = true, example = "1")
            @RequestParam Long userId,
            @ApiParam(value = "目标用户ID", required = true, example = "2")
            @PathVariable Long targetUserId) {
        try {
            userFollowService.unfollowUser(userId, targetUserId);
            return Result.success("取消关注成功");
        } catch (Exception e) {
            log.error("取消关注用户失败，用户ID: {}，目标用户ID: {}，错误: {}", userId, targetUserId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的关注者列表（粉丝）
     */
    @GetMapping("/followers")
    @ApiOperation(value = "获取粉丝列表", notes = "获取关注当前用户的用户列表")
    public Result<List<UserResponse>> getFollowers(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @RequestParam Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            List<UserResponse> followers = userFollowService.getFollowers(userId, page, size);
            return Result.success(followers);
        } catch (Exception e) {
            log.error("获取粉丝列表失败，用户ID: {}，错误: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户关注的用户列表
     */
    @GetMapping("/following")
    @ApiOperation(value = "获取关注列表", notes = "获取当前用户关注的用户列表")
    public Result<List<UserResponse>> getFollowing(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @RequestParam Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            List<UserResponse> following = userFollowService.getFollowing(userId, page, size);
            return Result.success(following);
        } catch (Exception e) {
            log.error("获取关注列表失败，用户ID: {}，错误: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查是否关注
     */
    @GetMapping("/check")
    @ApiOperation(value = "检查是否关注", notes = "检查当前用户是否关注了目标用户")
    public Result<Boolean> checkFollowing(
            @ApiParam(value = "当前用户ID", required = true, example = "1")
            @RequestParam Long userId,
            @ApiParam(value = "目标用户ID", required = true, example = "2")
            @RequestParam Long targetUserId) {
        try {
            boolean isFollowing = userFollowService.isFollowing(userId, targetUserId);
            return Result.success(isFollowing);
        } catch (Exception e) {
            log.error("检查关注状态失败，用户ID: {}，目标用户ID: {}，错误: {}", userId, targetUserId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的关注数量
     */
    @GetMapping("/{userId}/following/count")
    @ApiOperation(value = "获取关注数量", notes = "获取用户关注的用户数量")
    public Result<Long> getFollowingCount(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            long count = userFollowService.countFollowing(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取关注数量失败，用户ID: {}，错误: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的粉丝数量
     */
    @GetMapping("/{userId}/followers/count")
    @ApiOperation(value = "获取粉丝数量", notes = "获取关注该用户的用户数量")
    public Result<Long> getFollowersCount(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            long count = userFollowService.countFollowers(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取粉丝数量失败，用户ID: {}，错误: {}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
} 