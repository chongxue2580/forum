package com.example.blog_froum.service;

import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.RegisterRequest;
import com.example.blog_froum.dto.statistics.UserStatisticsResponse;
import com.example.blog_froum.dto.user.TwoFactorSetupResponse;
import com.example.blog_froum.dto.user.TwoFactorStatusResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import com.example.blog_froum.dto.user.ProfileUpdateRequest;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    UserResponse register(RegisterRequest request);

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据ID获取用户信息
     */
    UserResponse getUserById(Long id);

    /**
     * 根据用户名获取用户信息
     */
    UserResponse getUserByUsername(String username);

    /**
     * 更新用户信息
     */
    UserResponse updateUser(Long id, User user);

    /**
     * 更新用户状态
     */
    boolean updateUserStatus(Long id, UserStatus status);

    /**
     * 更新用户角色
     */
    boolean updateUserRole(Long id, UserRole role);

    /**
     * 删除用户
     */
    boolean deleteUser(Long id);

    /**
     * 获取所有用户（分页）
     */
    List<UserResponse> getAllUsers(int page, int size);

    /**
     * 根据状态获取用户（分页）
     */
    List<UserResponse> getUsersByStatus(UserStatus status, int page, int size);

    /**
     * 根据角色获取用户（分页）
     */
    List<UserResponse> getUsersByRole(UserRole role, int page, int size);

    /**
     * 统计用户总数
     */
    long countAllUsers();

    /**
     * 根据状态统计用户数
     */
    long countUsersByStatus(UserStatus status);

    /**
     * 根据角色统计用户数
     */
    long countUsersByRole(UserRole role);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 验证用户凭据
     */
    Optional<User> validateUser(String username, String password);

    /**
     * 验证用户凭据并检查账号状态
     */
    User authenticateUser(String username, String password);

    /**
     * 完成登录并签发JWT
     */
    LoginResponse completeLogin(User user);

    /**
     * 使用临时令牌完成两步验证登录
     */
    LoginResponse completeTwoFactorLogin(String twoFactorToken, String code, boolean requireAdmin);

    /**
     * 管理员首次登录发起两步验证绑定
     */
    LoginResponse beginAdminTwoFactorSetup(User user);

    /**
     * 管理员首次登录确认两步验证绑定
     */
    LoginResponse confirmAdminTwoFactorSetup(String setupToken, String code);

    /**
     * 获取当前用户两步验证状态
     */
    TwoFactorStatusResponse getTwoFactorStatus(Long userId);

    /**
     * 当前用户发起两步验证绑定
     */
    TwoFactorSetupResponse beginTwoFactorSetup(Long userId);

    /**
     * 当前用户确认启用两步验证
     */
    TwoFactorStatusResponse enableTwoFactor(Long userId, String code);

    /**
     * 当前用户关闭两步验证
     */
    TwoFactorStatusResponse disableTwoFactor(Long userId, String code);

    /**
     * 更新用户密码
     */
    boolean updatePassword(Long userId, String currentPassword, String newPassword);

    /**
     * 更新用户资料
     */
    UserResponse updateProfile(Long userId, ProfileUpdateRequest request);

    // 管理员功能
    /**
     * 获取所有用户（管理员）
     */
    org.springframework.data.domain.Page<UserResponse> getAllUsersForAdmin(
            org.springframework.data.domain.Pageable pageable, String role, String status);

    /**
     * 禁用用户
     */
    void disableUser(Long userId, String reason);

    /**
     * 启用用户
     */
    void enableUser(Long userId);

    /**
     * 重置用户密码
     */
    String resetUserPassword(Long userId);

    /**
     * 修改用户角色
     */
    void changeUserRole(Long userId, String role);

    /**
     * 管理员删除用户
     */
    void adminDeleteUser(Long userId);

    /**
     * 批量禁用用户
     */
    com.example.blog_froum.dto.admin.BatchOperationResult batchDisableUsers(java.util.List<Long> userIds, String reason);

    /**
     * 批量启用用户
     */
    com.example.blog_froum.dto.admin.BatchOperationResult batchEnableUsers(java.util.List<Long> userIds);

    /**
     * 批量删除用户
     */
    com.example.blog_froum.dto.admin.BatchOperationResult batchDeleteUsers(java.util.List<Long> userIds);

    /**
     * 获取用户统计信息
     */
    UserStatisticsResponse getUserStatistics();

    /**
     * 搜索用户
     */
    org.springframework.data.domain.Page<UserResponse> searchUsers(String keyword, org.springframework.data.domain.Pageable pageable);

    /**
     * 获取最近注册的用户
     */
    org.springframework.data.domain.Page<UserResponse> getRecentUsers(org.springframework.data.domain.Pageable pageable);
}
