package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.question.QuestionResponse;
import com.example.blog_froum.dto.user.EmailCodeRequest;
import com.example.blog_froum.dto.user.ForgotPasswordCodeRequest;
import com.example.blog_froum.dto.user.ForgotPasswordResetRequest;
import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.RegisterRequest;
import com.example.blog_froum.dto.user.UserStatsResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.dto.user.UserSummaryResponse;
import com.example.blog_froum.dto.user.PasswordUpdateRequest;
import com.example.blog_froum.dto.user.ProfileUpdateRequest;
import com.example.blog_froum.service.QuestionService;
import com.example.blog_froum.service.CaptchaService;
import com.example.blog_froum.service.EmailVerificationService;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.service.UserStatsService;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 发送注册邮箱验证码
     */
    @PostMapping("/register/email-code")
    @ApiOperation(value = "发送注册邮箱验证码", notes = "向注册邮箱发送6位验证码")
    public Result<Void> sendRegistrationEmailCode(
            @ApiParam(value = "邮箱信息", required = true)
            @Valid @RequestBody EmailCodeRequest request) {
        try {
            emailVerificationService.sendRegistrationCode(request.getEmail());
            return Result.success("验证码已发送，请查收邮箱");
        } catch (Exception e) {
            log.error("发送注册邮箱验证码失败，邮箱: {}, 错误: {}", request.getEmail(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送忘记密码邮箱验证码
     */
    @PostMapping("/forgot-password/email-code")
    @ApiOperation(value = "发送忘记密码验证码", notes = "根据用户名或邮箱查找绑定邮箱并发送6位验证码")
    public Result<Void> sendForgotPasswordEmailCode(
            @ApiParam(value = "账号信息", required = true)
            @Valid @RequestBody ForgotPasswordCodeRequest request) {
        try {
            emailVerificationService.sendPasswordResetCode(request.getAccount());
            return Result.success("验证码已发送，请查收绑定邮箱");
        } catch (Exception e) {
            log.error("发送忘记密码验证码失败，账号: {}, 错误: {}", request.getAccount(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 忘记密码重置
     */
    @PostMapping("/forgot-password/reset")
    @ApiOperation(value = "忘记密码重置", notes = "校验邮箱验证码后重置密码")
    public Result<Void> resetForgottenPassword(
            @ApiParam(value = "重置密码信息", required = true)
            @Valid @RequestBody ForgotPasswordResetRequest request) {
        try {
            emailVerificationService.resetPassword(request.getAccount(), request.getVerificationCode(), request.getNewPassword());
            return Result.success("密码重置成功");
        } catch (Exception e) {
            log.error("忘记密码重置失败，账号: {}, 错误: {}", request.getAccount(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "注册新用户账号，必须提供用户名、密码和邮箱")
    public Result<UserResponse> register(
            @ApiParam(value = "注册信息", required = true) 
            @Valid @RequestBody RegisterRequest request) {
        try {
            captchaService.validate(request.getCaptchaId(), request.getCaptchaPercentage());
            emailVerificationService.verifyRegistrationCode(request.getEmail(), request.getVerificationCode());
            log.info("收到用户注册请求，用户名: {}, 邮箱: {}", request.getUsername(), request.getEmail());
            UserResponse userResponse = userService.register(request);
            log.info("用户注册成功，用户名: {}", request.getUsername());
            return Result.success("注册成功", userResponse);
        } catch (Exception e) {
            log.error("用户注册失败，用户名: {}, 错误: {}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "使用用户名/邮箱和密码登录系统")
    public Result<LoginResponse> login(
            @ApiParam(value = "登录信息", required = true)
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            if (!StringUtils.hasText(request.getTwoFactorToken())) {
                captchaService.validate(request.getCaptchaId(), request.getCaptchaPercentage());
            }
            log.info("收到用户登录请求，用户名/邮箱: {}", request.getUsername());
            LoginResponse loginResponse = userService.login(request);
            String message = Boolean.TRUE.equals(loginResponse.getRequiresTwoFactor()) ? "需要两步验证码" : "登录成功";
            if (!Boolean.TRUE.equals(loginResponse.getRequiresTwoFactor())
                    && !Boolean.TRUE.equals(loginResponse.getRequiresTwoFactorSetup())
                    && loginResponse.getUser() != null) {
                UserResponse user = loginResponse.getUser();
                operationLogService.record(user.getId(), "USER_LOGIN", "login",
                        "用户登录系统", "USER", user.getId(), user.getNickname(), httpRequest);
            }
            log.info("用户登录处理完成，用户名/邮箱: {}, 状态: {}", request.getUsername(), message);
            return Result.success(message, loginResponse);
        } catch (Exception e) {
            log.error("用户登录失败，用户名/邮箱: {}, 错误: {}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/profile/{userId}")
    @ApiOperation(value = "获取用户资料", notes = "根据用户ID获取用户个人资料")
    public Result<UserResponse> getUserProfile(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            UserResponse userResponse = userService.getUserById(userId);
            return Result.success(userResponse);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取公开用户推荐列表
     */
    @GetMapping("/recent-users")
    @ApiOperation(value = "获取最近加入用户", notes = "首页推荐作者使用的公开用户摘要列表")
    public Result<List<UserSummaryResponse>> getRecentUsers(
            @ApiParam(value = "数量", example = "4")
            @RequestParam(defaultValue = "4") int limit) {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 12));
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, safeLimit);
            List<UserSummaryResponse> users = userService.getRecentUsers(pageable)
                    .getContent()
                    .stream()
                    .map(UserSummaryResponse::fromUserResponse)
                    .collect(Collectors.toList());
            return Result.success(users);
        } catch (Exception e) {
            log.error("获取最近用户失败: {}", e.getMessage(), e);
            return Result.error("获取最近用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/profile/{userId}/stats")
    @ApiOperation(value = "获取用户统计数据", notes = "获取用户的文章数、问答数、回答数、关注者数等统计信息")
    public Result<UserStatsResponse> getUserStats(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            log.info("获取用户统计数据，用户ID: {}", userId);

            // 调用统计服务获取真实数据
            UserStatsResponse stats = userStatsService.getUserStats(userId);

            log.info("用户统计数据获取成功，用户ID: {}, 统计结果: {}", userId, stats);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取用户统计数据失败，用户ID: {}，错误: {}", userId, e.getMessage(), e);
            return Result.error("获取用户统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的问答列表
     */
    @GetMapping("/profile/{userId}/questions")
    @ApiOperation(value = "获取用户问答列表", notes = "获取指定用户发布的问答列表")
    public Result<org.springframework.data.domain.Page<QuestionResponse>> getUserQuestions(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @ApiParam(value = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取用户问答列表，用户ID: {}, 页码: {}, 大小: {}", userId, page, size);

            org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                    org.springframework.data.domain.Sort.by("createdAt").descending());

            org.springframework.data.domain.Page<QuestionResponse> questions =
                questionService.getUserQuestionsByStatus(userId, "PUBLISHED", pageable);

            log.info("用户问答列表获取成功，用户ID: {}, 问答数量: {}", userId, questions.getTotalElements());
            return Result.success(questions);
        } catch (Exception e) {
            log.error("获取用户问答列表失败，用户ID: {}，错误: {}", userId, e.getMessage(), e);
            return Result.error("获取用户问答列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile/{userId}")
    @ApiOperation(value = "更新用户资料", notes = "更新指定用户的个人资料信息")
    public Result<UserResponse> updateProfile(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @ApiParam(value = "用户资料", required = true)
            @Valid @RequestBody ProfileUpdateRequest request) {
        try {
            UserResponse currentUser = userService.getUserById(userId);
            if (isEmailChanged(currentUser.getEmail(), request.getEmail())) {
                emailVerificationService.verifyEmailChangeCode(request.getEmail(), request.getVerificationCode());
            }

            UserResponse updatedUser = userService.updateProfile(userId, request);
            return Result.success("资料更新成功", updatedUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    @ApiOperation(value = "检查用户名可用性", notes = "检查指定用户名是否已被注册")
    public Result<Boolean> checkUsername(
            @ApiParam(value = "待检查的用户名", required = true, example = "user123")
            @RequestParam String username) {
        try {
            boolean exists = userService.existsByUsername(username);
            return Result.success(!exists); // 返回true表示可用，false表示已存在
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    @ApiOperation(value = "检查邮箱可用性", notes = "检查指定邮箱是否已被注册")
    public Result<Boolean> checkEmail(
            @ApiParam(value = "待检查的邮箱", required = true, example = "user@example.com")
            @RequestParam String email) {
        try {
            boolean exists = userService.existsByEmail(email);
            return Result.success(!exists); // 返回true表示可用，false表示已存在
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户密码
     */
    @PutMapping("/profile/{userId}/password")
    @ApiOperation(value = "更新用户密码", notes = "更新指定用户的登录密码")
    public Result<Void> updatePassword(
            @ApiParam(value = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @ApiParam(value = "密码更新信息", required = true)
            @Valid @RequestBody PasswordUpdateRequest request) {
        try {
            emailVerificationService.verifyPasswordChangeCode(userId, request.getVerificationCode());
            userService.updatePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            return Result.success("密码更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private boolean isEmailChanged(String currentEmail, String newEmail) {
        if (!StringUtils.hasText(newEmail)) {
            return false;
        }

        String normalizedCurrent = currentEmail == null ? "" : currentEmail.trim();
        return !newEmail.trim().equalsIgnoreCase(normalizedCurrent);
    }
}
