package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.statistics.UserStatisticsResponse;
import com.example.blog_froum.dto.user.UserArticleSummaryResponse;
import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.ProfileUpdateRequest;
import com.example.blog_froum.dto.user.RegisterRequest;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.JwtUtil;
import com.example.blog_froum.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.blog_froum.utils.PasswordUtil.passwordEncoder;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public UserResponse register(RegisterRequest request) {
        log.info("开始用户注册，用户名: {}, 邮箱: {}", request.getUsername(), request.getEmail());

        // 检查用户名是否已存在
        if (userMapper.existsByUsername(request.getUsername())) {
            log.warn("用户注册失败，用户名已存在: {}", request.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userMapper.existsByEmail(request.getEmail())) {
            log.warn("用户注册失败，邮箱已存在: {}", request.getEmail());
            throw new RuntimeException("邮箱已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(PasswordUtil.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setBio(request.getBio());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            log.error("用户注册失败，数据库插入失败，用户名: {}", request.getUsername());
            throw new RuntimeException("注册失败");
        }

        log.info("用户注册成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        return UserResponse.fromUser(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录尝试，用户名/邮箱: {}", request.getUsername());

        // 查找用户（支持用户名或邮箱登录）
        User user = userMapper.findByUsernameOrEmail(request.getUsername(), request.getUsername());
        if (user == null) {
            log.warn("用户登录失败，用户不存在: {}", request.getUsername());
            throw new RuntimeException("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("用户登录失败，账户状态异常: {}, 状态: {}", user.getUsername(), user.getStatus());
            throw new RuntimeException("账户已被禁用或锁定");
        }

        // 验证密码
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            log.warn("用户登录失败，密码错误: {}", user.getUsername());
            throw new RuntimeException("密码错误");
        }

        // 更新登录信息
        user.updateLoginInfo();
        userMapper.updateLoginInfo(user);

        // 生成JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().getCode());

        log.info("用户登录成功，用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), user.getRole());

        // 返回登录响应
        UserResponse userResponse = UserResponse.fromUser(user);
        return new LoginResponse(token, userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserResponse userResponse = UserResponse.fromUser(user);

        // 获取用户的文章列表（最新的10篇）。查询失败必须显式暴露，不能返回默认空列表掩盖数据异常。
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        List<UserArticleSummaryResponse> articleList =
                articleRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(id, "PUBLISHED", pageable)
                        .getContent()
                        .stream()
                        .map(article -> new UserArticleSummaryResponse(
                                article.getId(),
                                article.getTitle(),
                                article.getSummary(),
                                article.getCreatedAt(),
                                article.getViewCount(),
                                article.getLikeCount()))
                        .collect(Collectors.toList());

        userResponse.setArticles(articleList);
        log.info("获取用户文章成功，用户ID: {}, 文章数量: {}", id, articleList.size());

        return userResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return UserResponse.fromUser(user);
    }

    @Override
    public UserResponse updateUser(Long id, User updateUser) {
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新用户信息
        existingUser.setNickname(updateUser.getNickname());
        existingUser.setBio(updateUser.getBio());
        existingUser.setAvatarUrl(updateUser.getAvatarUrl());
        existingUser.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(existingUser);
        if (result <= 0) {
            throw new RuntimeException("更新失败");
        }

        return UserResponse.fromUser(existingUser);
    }

    @Override
    public boolean updateUserStatus(Long id, UserStatus status) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int result = userMapper.updateStatus(id, status);
        return result > 0;
    }

    @Override
    public boolean updateUserRole(Long id, UserRole role) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int result = userMapper.updateRole(id, role);
        return result > 0;
    }

    @Override
    public boolean deleteUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int result = userMapper.deleteById(id);
        return result > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(int page, int size) {
        int offset = page * size;
        List<User> users = userMapper.findAll(offset, size);
        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByStatus(UserStatus status, int page, int size) {
        int offset = page * size;
        List<User> users = userMapper.findByStatus(status, offset, size);
        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(UserRole role, int page, int size) {
        int offset = page * size;
        List<User> users = userMapper.findByRole(role, offset, size);
        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userMapper.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByStatus(UserStatus status) {
        return userMapper.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByRole(UserRole role) {
        return userMapper.countByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> validateUser(String username, String password) {
        User user = userMapper.findByUsernameOrEmail(username, username);
        if (user != null && PasswordUtil.matches(password, user.getPassword())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            log.warn("密码更新失败，用户不存在，用户ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 验证当前密码
        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            log.warn("密码更新失败，当前密码不正确，用户ID: {}", userId);
            throw new RuntimeException("当前密码不正确");
        }

        // 更新密码
        String encodedPassword = PasswordUtil.encode(newPassword);
        int result = userMapper.updatePassword(userId, encodedPassword);
        
        if (result <= 0) {
            log.error("密码更新失败，数据库更新失败，用户ID: {}", userId);
            throw new RuntimeException("密码更新失败");
        }

        log.info("密码更新成功，用户ID: {}", userId);
        return true;
    }

    @Override
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            log.warn("更新用户资料失败，用户不存在，用户ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 如果更新邮箱，需要检查邮箱是否已存在
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userMapper.existsByEmail(request.getEmail())) {
                log.warn("更新用户资料失败，邮箱已存在: {}", request.getEmail());
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }

        // 更新其他资料
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        
        if (request.getBio() != null) { // 允许设置为空字符串
            user.setBio(request.getBio());
        }
        
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(user);
        if (result <= 0) {
            log.error("更新用户资料失败，数据库更新失败，用户ID: {}", userId);
            throw new RuntimeException("更新资料失败");
        }

        log.info("更新用户资料成功，用户ID: {}", userId);
        return UserResponse.fromUser(user);
    }

    // 管理员功能实现
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> getAllUsersForAdmin(
            org.springframework.data.domain.Pageable pageable, String role, String status) {
        log.info("管理员获取用户列表，页码: {}, 每页数量: {}, 角色: {}, 状态: {}",
                pageable.getPageNumber(), pageable.getPageSize(), role, status);

        // 这里需要实现分页查询逻辑
        // 由于现有的userMapper不支持Spring Data的Pageable，我们需要转换
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        List<User> users;
        long total;

        if (role != null && !role.isEmpty() && status != null && !status.isEmpty()) {
            // 同时按角色和状态筛选
            users = userMapper.findByRoleAndStatus(UserRole.valueOf(role), UserStatus.valueOf(status), offset, size);
            total = userMapper.countByRoleAndStatus(UserRole.valueOf(role), UserStatus.valueOf(status));
        } else if (role != null && !role.isEmpty()) {
            // 按角色筛选
            users = userMapper.findByRole(UserRole.valueOf(role), offset, size);
            total = userMapper.countByRole(UserRole.valueOf(role));
        } else if (status != null && !status.isEmpty()) {
            // 按状态筛选
            users = userMapper.findByStatus(UserStatus.valueOf(status), offset, size);
            total = userMapper.countByStatus(UserStatus.valueOf(status));
        } else {
            // 获取所有用户
            users = userMapper.findAll(offset, size);
            total = userMapper.countAll();
        }

        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(userResponses, pageable, total);
    }

    @Override
    public void disableUser(Long userId, String reason) {
        log.info("管理员禁用用户，用户ID: {}, 原因: {}", userId, reason);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus(UserStatus.DISABLED);
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("禁用用户失败");
        }

        log.info("用户禁用成功，用户ID: {}", userId);
    }

    @Override
    public void enableUser(Long userId) {
        log.info("管理员启用用户，用户ID: {}", userId);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("启用用户失败");
        }

        log.info("用户启用成功，用户ID: {}", userId);
    }

    @Override
    public String resetUserPassword(Long userId) {
        log.info("管理员重置用户密码，用户ID: {}", userId);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 生成新密码
        String newPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(newPassword);

        user.setPassword(encodedPassword);
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("重置密码失败");
        }

        log.info("用户密码重置成功，用户ID: {}", userId);
        return newPassword;
    }

    @Override
    public void changeUserRole(Long userId, String role) {
        log.info("管理员修改用户角色，用户ID: {}, 新角色: {}", userId, role);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setRole(UserRole.valueOf(role));
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("修改角色失败");
        }

        log.info("用户角色修改成功，用户ID: {}, 新角色: {}", userId, role);
    }

    @Override
    public void adminDeleteUser(Long userId) {
        log.info("管理员删除用户，用户ID: {}", userId);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int result = userMapper.deleteById(userId);
        if (result <= 0) {
            throw new RuntimeException("删除用户失败");
        }

        log.info("用户删除成功，用户ID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics() {
        log.info("获取用户统计信息");

        // 统计各种用户数据
        long totalUsers = userMapper.countAll();
        long activeUsers = userMapper.countByStatus(UserStatus.ACTIVE);
        long disabledUsers = userMapper.countByStatus(UserStatus.DISABLED);
        long adminUsers = userMapper.countByRole(UserRole.ADMIN);
        long regularUsers = userMapper.countByRole(UserRole.USER);

        return new UserStatisticsResponse(totalUsers, activeUsers, disabledUsers, adminUsers, regularUsers);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> searchUsers(String keyword, org.springframework.data.domain.Pageable pageable) {
        log.info("搜索用户，关键词: {}", keyword);

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        // 这里需要在UserMapper中添加搜索方法
        List<User> users = userMapper.searchUsers(keyword, offset, size);
        long total = userMapper.countSearchUsers(keyword);

        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(userResponses, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> getRecentUsers(org.springframework.data.domain.Pageable pageable) {
        log.info("获取最近注册的用户");

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        List<User> users = userMapper.findRecentUsers(offset, size);
        long total = userMapper.countAll();

        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(userResponses, pageable, total);
    }

    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}
