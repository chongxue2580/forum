package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.statistics.UserStatisticsResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.UserService;
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
 * 管理员用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员用户管理")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取所有用户
     */
    @GetMapping
    @ApiOperation(value = "获取所有用户", notes = "分页获取系统中的所有用户")
    public Result<Page<UserResponse>> getAllUsers(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "角色筛选") @RequestParam(required = false) String role,
            @ApiParam(value = "状态筛选") @RequestParam(required = false) String status) {
        try {
            log.info("管理员获取用户列表，页码: {}, 每页数量: {}, 角色: {}, 状态: {}", page, pageSize, role, status);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            
            Page<UserResponse> responsePage = userService.getAllUsersForAdmin(pageable, role, status);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取用户详情", notes = "获取指定用户的详细信息")
    public Result<UserResponse> getUserById(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id) {
        try {
            log.info("管理员获取用户详情，ID: {}", id);
            UserResponse userResponse = userService.getUserById(id);
            return Result.success(userResponse);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取用户详情失败: " + e.getMessage());
        }
    }

    /**
     * 禁用用户
     */
    @PostMapping("/{id}/disable")
    @ApiOperation(value = "禁用用户", notes = "管理员禁用指定用户")
    public Result<Void> disableUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            @ApiParam(value = "禁用原因") @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        try {
            log.info("管理员禁用用户，ID: {}, 原因: {}", id, reason);
            userService.disableUser(id, reason);
            operationLogService.record(BaseContext.getCurrentId(), "USER_MANAGEMENT", "disable",
                    "禁用用户：" + id + (reason == null ? "" : "，原因：" + reason), "USER", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("禁用用户失败", e);
            return Result.error("禁用用户失败: " + e.getMessage());
        }
    }

    /**
     * 启用用户
     */
    @PostMapping("/{id}/enable")
    @ApiOperation(value = "启用用户", notes = "管理员启用指定用户")
    public Result<Void> enableUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员启用用户，ID: {}", id);
            userService.enableUser(id);
            operationLogService.record(BaseContext.getCurrentId(), "USER_MANAGEMENT", "enable",
                    "启用用户：" + id, "USER", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("启用用户失败", e);
            return Result.error("启用用户失败: " + e.getMessage());
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @ApiOperation(value = "重置用户密码", notes = "管理员重置用户密码")
    public Result<String> resetUserPassword(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员重置用户密码，ID: {}", id);
            String newPassword = userService.resetUserPassword(id);
            operationLogService.record(BaseContext.getCurrentId(), "USER_MANAGEMENT", "reset_password",
                    "重置用户密码：" + id, "USER", id, null, request);
            return Result.success("密码重置成功", newPassword);
        } catch (Exception e) {
            log.error("重置用户密码失败", e);
            return Result.error("重置用户密码失败: " + e.getMessage());
        }
    }

    /**
     * 修改用户角色
     */
    @PostMapping("/{id}/role")
    @ApiOperation(value = "修改用户角色", notes = "管理员修改用户角色")
    public Result<Void> changeUserRole(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            @ApiParam(value = "新角色", required = true) @RequestParam String role,
            HttpServletRequest request) {
        try {
            log.info("管理员修改用户角色，ID: {}, 新角色: {}", id, role);
            userService.changeUserRole(id, role);
            operationLogService.record(BaseContext.getCurrentId(), "USER_MANAGEMENT", "change_role",
                    "修改用户角色：" + id + " -> " + role, "USER", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("修改用户角色失败", e);
            return Result.error("修改用户角色失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除用户", notes = "管理员删除指定用户")
    public Result<Void> deleteUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {
        try {
            log.info("管理员删除用户，ID: {}", id);
            userService.adminDeleteUser(id);
            operationLogService.record(BaseContext.getCurrentId(), "USER_MANAGEMENT", "delete",
                    "删除用户：" + id, "USER", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取用户统计信息", notes = "获取用户的各种统计数据")
    public Result<UserStatisticsResponse> getUserStatistics() {
        try {
            log.info("管理员获取用户统计信息");
            UserStatisticsResponse statistics = userService.getUserStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @ApiOperation(value = "搜索用户", notes = "根据关键词搜索用户")
    public Result<Page<UserResponse>> searchUsers(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("管理员搜索用户，关键词: {}", keyword);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<UserResponse> responsePage = userService.searchUsers(keyword, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return Result.error("搜索用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近注册的用户
     */
    @GetMapping("/recent")
    @ApiOperation(value = "获取最近注册用户", notes = "获取最近注册的用户列表")
    public Result<Page<UserResponse>> getRecentUsers(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "20") @RequestParam(defaultValue = "20") int pageSize) {
        try {
            log.info("管理员获取最近注册用户");
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<UserResponse> responsePage = userService.getRecentUsers(pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取最近注册用户失败", e);
            return Result.error("获取最近注册用户失败: " + e.getMessage());
        }
    }
}
