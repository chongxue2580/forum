package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员功能")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    @ApiOperation(value = "管理员登录", notes = "管理员账号登录系统，只有管理员角色的用户才能登录成功")
    public Result<LoginResponse> adminLogin(
            @ApiParam(value = "登录信息", required = true)
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("收到管理员登录请求，用户名/邮箱: {}", request.getUsername());

            // 先进行普通登录验证
            LoginResponse loginResponse = userService.login(request);

            // 检查用户是否为管理员
            UserResponse user = loginResponse.getUser();
            if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN) {
                log.warn("管理员登录失败，权限不足: {}, 角色: {}", user.getUsername(), user.getRole());
                return Result.error("权限不足，非管理员账户");
            }

            log.info("管理员登录成功，用户名: {}, 角色: {}", user.getUsername(), user.getRole());
            operationLogService.record(user.getId(), "USER_LOGIN", "login",
                    "管理员登录系统", "USER", user.getId(), user.getNickname(), httpRequest);
            return Result.success("管理员登录成功", loginResponse);
        } catch (Exception e) {
            log.error("管理员登录失败，用户名/邮箱: {}, 错误: {}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // 用户管理方法已移至 AdminUserController

    // 禁用用户方法已移至 AdminUserController

    // 启用用户方法已移至 AdminUserController

    // 用户角色管理方法已移至 AdminUserController

    // 删除用户方法已移至 AdminUserController

    // 用户统计方法已移至 AdminUserController

    // UserStats类已移至相应的DTO包中
}
