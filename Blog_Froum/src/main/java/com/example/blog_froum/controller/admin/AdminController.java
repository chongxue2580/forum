package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.admin.AdminTwoFactorSetupConfirmRequest;
import com.example.blog_froum.dto.admin.AdminTwoFactorEmailCodeRequest;
import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
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

            LoginResponse loginResponse;

            if (StringUtils.hasText(request.getTwoFactorToken())) {
                if ("email".equalsIgnoreCase(request.getTwoFactorMethod())) {
                    loginResponse = userService.completeAdminTwoFactorEmailLogin(
                            request.getTwoFactorToken(),
                            request.getTwoFactorCode());
                } else {
                    loginResponse = userService.completeTwoFactorLogin(
                            request.getTwoFactorToken(),
                            request.getTwoFactorCode(),
                            true);
                }
            } else {
                User admin = userService.authenticateUser(request.getUsername(), request.getPassword());

                if (!admin.isAdmin()) {
                    log.warn("管理员登录失败，权限不足: {}, 角色: {}", admin.getUsername(), admin.getRole());
                    return Result.error("权限不足，非管理员账户");
                }

                if (!admin.isTwoFactorActive()) {
                    LoginResponse setupResponse = userService.beginAdminTwoFactorSetup(admin);
                    return Result.success("管理员首次登录需要绑定两步验证", setupResponse);
                }

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername(request.getUsername());
                loginRequest.setPassword(request.getPassword());
                loginRequest.setTwoFactorCode(request.getTwoFactorCode());
                loginResponse = userService.login(loginRequest);
            }

            // 检查用户是否为管理员
            UserResponse user = loginResponse.getUser();
            if (Boolean.TRUE.equals(loginResponse.getRequiresTwoFactor())) {
                return Result.success("需要两步验证码", loginResponse);
            }
            if (Boolean.TRUE.equals(loginResponse.getRequiresTwoFactorSetup())) {
                return Result.success("管理员首次登录需要绑定两步验证", loginResponse);
            }
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

    @PostMapping("/2fa/email-code")
    @ApiOperation(value = "发送管理员登录邮箱验证码", notes = "账号密码校验通过后，可使用两步验证临时令牌发送邮箱验证码")
    public Result<Void> sendAdminTwoFactorEmailCode(
            @Valid @RequestBody AdminTwoFactorEmailCodeRequest request) {
        try {
            userService.sendAdminTwoFactorEmailCode(request.getTwoFactorToken());
            return Result.success("验证码已发送，请查收管理员邮箱");
        } catch (Exception e) {
            log.error("管理员邮箱验证码发送失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/2fa/setup/confirm")
    @ApiOperation(value = "确认管理员首次两步验证绑定", notes = "管理员首次登录时校验验证器验证码，成功后签发登录令牌")
    public Result<LoginResponse> confirmAdminTwoFactorSetup(
            @Valid @RequestBody AdminTwoFactorSetupConfirmRequest request,
            HttpServletRequest httpRequest) {
        try {
            LoginResponse loginResponse = userService.confirmAdminTwoFactorSetup(request.getSetupToken(), request.getCode());
            UserResponse user = loginResponse.getUser();
            operationLogService.record(user.getId(), "USER_LOGIN", "login",
                    "管理员首次绑定两步验证并登录系统", "USER", user.getId(), user.getNickname(), httpRequest);
            return Result.success("两步验证绑定成功，管理员登录成功", loginResponse);
        } catch (Exception e) {
            log.error("管理员两步验证绑定确认失败: {}", e.getMessage());
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
