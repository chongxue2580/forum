package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.ProfileUpdateRequest;
import com.example.blog_froum.dto.user.RegisterRequest;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户个人资料控制器 - 需要JWT认证
 */
@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "用户个人资料管理")
public class UserProfileController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录 - 兼容前端调用
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "使用用户名/邮箱和密码登录系统")
    public Result<LoginResponse> login(
            @ApiParam(value = "登录信息", required = true)
            @Valid @RequestBody LoginRequest request) {
        try {
            log.info("收到用户登录请求（profile路径），用户名/邮箱: {}", request.getUsername());
            LoginResponse loginResponse = userService.login(request);
            log.info("用户登录成功（profile路径），用户名/邮箱: {}", request.getUsername());
            return Result.success("登录成功", loginResponse);
        } catch (Exception e) {
            log.error("用户登录失败（profile路径），用户名/邮箱: {}, 错误: {}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册 - 兼容前端调用
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "注册新用户账号，必须提供用户名、密码和邮箱")
    public Result<UserResponse> register(
            @ApiParam(value = "注册信息", required = true)
            @Valid @RequestBody RegisterRequest request) {
        try {
            log.info("收到用户注册请求（profile路径），用户名: {}, 邮箱: {}", request.getUsername(), request.getEmail());
            UserResponse userResponse = userService.register(request);
            log.info("用户注册成功（profile路径），用户名: {}", request.getUsername());
            return Result.success("注册成功", userResponse);
        } catch (Exception e) {
            log.error("用户注册失败（profile路径），用户名: {}, 错误: {}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @ApiOperation(value = "获取当前用户信息", notes = "获取当前登录用户的详细信息")
    public Result<UserResponse> getCurrentUser(HttpServletRequest request) {
        try {
            // 从ThreadLocal获取当前用户ID
            Long currentUserId = BaseContext.getCurrentId();
            
            // 也可以从请求属性中获取
            Long userIdFromRequest = (Long) request.getAttribute("userId");
            String username = (String) request.getAttribute("username");
            String role = (String) request.getAttribute("role");
            
            log.info("获取当前用户信息，ThreadLocal用户ID: {}, 请求属性用户ID: {}, 用户名: {}, 角色: {}", 
                    currentUserId, userIdFromRequest, username, role);
            
            UserResponse userResponse = userService.getUserById(currentUserId);
            return Result.success("获取用户信息成功", userResponse);
        } catch (Exception e) {
            log.error("获取当前用户信息失败: {}", e.getMessage());
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    @ApiOperation(value = "更新当前用户信息", notes = "更新当前登录用户的个人信息")
    public Result<UserResponse> updateCurrentUser(
            @Valid @RequestBody ProfileUpdateRequest updateRequest,
            HttpServletRequest request) {
        try {
            Long currentUserId = BaseContext.getCurrentId();
            String username = (String) request.getAttribute("username");
            
            log.info("用户 {} (ID: {}) 尝试更新个人信息", username, currentUserId);

            UserResponse updatedUser = userService.updateProfile(currentUserId, updateRequest);
            return Result.success("用户信息更新成功", updatedUser);
        } catch (Exception e) {
            log.error("更新用户信息失败: {}", e.getMessage());
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 测试JWT认证的接口
     */
    @GetMapping("/test-auth")
    @ApiOperation(value = "测试JWT认证", notes = "测试JWT认证是否正常工作")
    public Result<String> testAuth(HttpServletRequest request) {
        Long userId = BaseContext.getCurrentId();
        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");
        
        String message = String.format("JWT认证成功！用户ID: %d, 用户名: %s, 角色: %s", userId, username, role);
        log.info(message);
        
        return Result.success("JWT认证成功", message);
    }
}
