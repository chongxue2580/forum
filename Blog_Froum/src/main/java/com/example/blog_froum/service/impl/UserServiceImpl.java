package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.statistics.UserStatisticsResponse;
import com.example.blog_froum.dto.admin.AdminUserBanRequest;
import com.example.blog_froum.dto.admin.AdminUserDetailResponse;
import com.example.blog_froum.dto.admin.BatchOperationResult;
import com.example.blog_froum.dto.user.UserArticleSummaryResponse;
import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.ProfileUpdateRequest;
import com.example.blog_froum.dto.user.RegisterRequest;
import com.example.blog_froum.dto.user.TwoFactorSetupResponse;
import com.example.blog_froum.dto.user.TwoFactorStatusResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.OperationLog;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserBanType;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.FollowRepository;
import com.example.blog_froum.repository.LikeRepository;
import com.example.blog_froum.repository.OperationLogRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.service.TwoFactorService;
import com.example.blog_froum.service.EmailVerificationService;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.JwtUtil;
import com.example.blog_froum.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private FollowRepository followRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    private static final DateTimeFormatter BAN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public UserResponse register(RegisterRequest request) {
        String username = normalizeAccount(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        log.info("开始用户注册，用户名: {}, 邮箱: {}", username, email);

        // 检查用户名是否已存在
        if (userMapper.existsByUsername(username)) {
            log.warn("用户注册失败，用户名已存在: {}", username);
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userMapper.existsByEmail(email)) {
            log.warn("用户注册失败，邮箱已存在: {}", email);
            throw new RuntimeException("邮箱已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(PasswordUtil.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setBio(StringUtils.hasText(request.getBio()) ? request.getBio().trim() : null);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setLoginCount(0);
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            log.error("用户注册失败，数据库插入失败，用户名: {}", username);
            throw new RuntimeException("注册失败");
        }

        log.info("用户注册成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        return UserResponse.fromUser(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录尝试，用户名/邮箱: {}", request.getUsername());

        if (StringUtils.hasText(request.getTwoFactorToken())) {
            return completeTwoFactorLogin(request.getTwoFactorToken(), request.getTwoFactorCode(), false);
        }

        User user = authenticateUser(request.getUsername(), request.getPassword());

        if (user.isTwoFactorActive()) {
            if (StringUtils.hasText(request.getTwoFactorCode())) {
                if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                    log.warn("用户登录失败，两步验证码错误: {}", user.getUsername());
                    throw new RuntimeException("两步验证码错误");
                }
                return completeLogin(user);
            }

            String token = twoFactorService.createLoginChallenge(user.getId());
            return LoginResponse.requiresTwoFactor(token, UserResponse.fromUser(user));
        }

        return completeLogin(user);
    }

    @Override
    public User authenticateUser(String username, String password) {
        String account = normalizeAccount(username);
        // 查找用户（支持用户名或邮箱登录）
        User user = userMapper.findByUsernameOrEmail(account, account);
        if (user == null) {
            log.warn("用户登录失败，用户不存在: {}", account);
            throw new RuntimeException("用户不存在");
        }

        assertCanLogin(user);

        // 验证密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            log.warn("用户登录失败，密码错误: {}", user.getUsername());
            throw new RuntimeException("密码错误");
        }

        return user;
    }

    @Override
    public LoginResponse completeLogin(User user) {
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
    public void assertCanUseAccount(Long userId) {
        User user = getExistingUser(userId);
        assertCanLogin(user);
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
    public AdminUserDetailResponse getAdminUserDetail(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        clearExpiredBanIfNecessary(user);

        OperationLog latestAccessLog = operationLogRepository.findFirstByUserIdOrderByCreatedAtDesc(id).orElse(null);
        long articleCount = articleRepository.countByAuthorId(id);
        long questionCount = questionRepository.countByAuthorId(id);
        long commentCount = commentRepository.countByUserIdAndIsDeletedFalse(id);
        long likeCount = likeRepository.countByUserId(id);
        long followingCount = followRepository.countByFollowerIdAndTargetType(id, "USER");
        long followerCount = followRepository.countByTargetIdAndTargetType(id, "USER");

        return AdminUserDetailResponse.from(
                user,
                latestAccessLog,
                articleCount,
                questionCount,
                commentCount,
                likeCount,
                followingCount,
                followerCount);
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

        removeUserAsFollowTarget(id);
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
        String account = normalizeAccount(username);
        User user = userMapper.findByUsernameOrEmail(account, account);
        if (user != null && PasswordUtil.matches(password, user.getPassword())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public LoginResponse completeTwoFactorLogin(String twoFactorToken, String code, boolean requireAdmin) {
        TwoFactorService.PendingChallenge challenge = twoFactorService.getChallenge(
                twoFactorToken,
                TwoFactorService.ChallengeType.LOGIN);
        if (challenge == null) {
            throw new RuntimeException("两步验证已过期，请重新登录");
        }

        User user = userMapper.findById(challenge.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        assertCanLogin(user);
        if (requireAdmin && !user.isAdmin()) {
            throw new RuntimeException("权限不足，非管理员账户");
        }
        if (!user.isTwoFactorActive()) {
            throw new RuntimeException("该账号尚未启用两步验证");
        }
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("两步验证码错误");
        }

        twoFactorService.consumeChallenge(twoFactorToken);
        return completeLogin(user);
    }

    @Override
    public void sendAdminTwoFactorEmailCode(String twoFactorToken) {
        TwoFactorService.PendingChallenge challenge = getLoginChallenge(twoFactorToken);
        User user = getExistingUser(challenge.getUserId());
        assertCanLogin(user);
        if (!user.isAdmin()) {
            throw new RuntimeException("权限不足，非管理员账户");
        }

        emailVerificationService.sendAdminTwoFactorCode(user.getId());
    }

    @Override
    public LoginResponse completeAdminTwoFactorEmailLogin(String twoFactorToken, String code) {
        TwoFactorService.PendingChallenge challenge = getLoginChallenge(twoFactorToken);
        User user = getExistingUser(challenge.getUserId());
        assertCanLogin(user);
        if (!user.isAdmin()) {
            throw new RuntimeException("权限不足，非管理员账户");
        }

        emailVerificationService.verifyAdminTwoFactorCode(user.getId(), code);
        twoFactorService.consumeChallenge(twoFactorToken);
        return completeLogin(user);
    }

    @Override
    public LoginResponse beginAdminTwoFactorSetup(User user) {
        String secret = twoFactorService.generateSecret();
        String setupToken = twoFactorService.createAdminSetupChallenge(user.getId(), secret);
        String otpAuthUrl = twoFactorService.buildOtpAuthUrl(user.getUsername(), secret);
        return LoginResponse.requiresTwoFactorSetup(
                setupToken,
                secret,
                otpAuthUrl,
                UserResponse.fromUser(user));
    }

    @Override
    public LoginResponse confirmAdminTwoFactorSetup(String setupToken, String code) {
        TwoFactorService.PendingChallenge challenge = twoFactorService.getChallenge(
                setupToken,
                TwoFactorService.ChallengeType.ADMIN_SETUP);
        if (challenge == null) {
            throw new RuntimeException("两步验证绑定已过期，请重新登录");
        }
        if (!twoFactorService.verifyCode(challenge.getSecret(), code)) {
            throw new RuntimeException("两步验证码错误");
        }

        User user = userMapper.findById(challenge.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        assertCanLogin(user);
        if (!user.isAdmin()) {
            throw new RuntimeException("权限不足，非管理员账户");
        }

        int result = userMapper.updateTwoFactor(user.getId(), true, challenge.getSecret());
        if (result <= 0) {
            throw new RuntimeException("两步验证绑定失败");
        }

        twoFactorService.consumeChallenge(setupToken);
        User updatedUser = userMapper.findById(user.getId());
        return completeLogin(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorStatusResponse getTwoFactorStatus(Long userId) {
        User user = getExistingUser(userId);
        return new TwoFactorStatusResponse(user.isTwoFactorActive());
    }

    @Override
    public TwoFactorSetupResponse beginTwoFactorSetup(Long userId) {
        User user = getExistingUser(userId);
        if (user.isTwoFactorActive()) {
            return new TwoFactorSetupResponse(true, null, null);
        }

        String secret = twoFactorService.generateSecret();
        int result = userMapper.updateTwoFactor(userId, false, secret);
        if (result <= 0) {
            throw new RuntimeException("两步验证初始化失败");
        }
        String otpAuthUrl = twoFactorService.buildOtpAuthUrl(user.getUsername(), secret);
        return new TwoFactorSetupResponse(false, secret, otpAuthUrl);
    }

    @Override
    public TwoFactorStatusResponse enableTwoFactor(Long userId, String code) {
        User user = getExistingUser(userId);
        if (!StringUtils.hasText(user.getTwoFactorSecret())) {
            throw new RuntimeException("请先生成两步验证密钥");
        }
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("两步验证码错误");
        }

        int result = userMapper.updateTwoFactor(userId, true, user.getTwoFactorSecret());
        if (result <= 0) {
            throw new RuntimeException("启用两步验证失败");
        }
        return new TwoFactorStatusResponse(true);
    }

    @Override
    public TwoFactorStatusResponse disableTwoFactor(Long userId, String code) {
        User user = getExistingUser(userId);
        if (!user.isTwoFactorActive()) {
            return new TwoFactorStatusResponse(false);
        }
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("两步验证码错误");
        }

        int result = userMapper.updateTwoFactor(userId, false, null);
        if (result <= 0) {
            throw new RuntimeException("关闭两步验证失败");
        }
        return new TwoFactorStatusResponse(false);
    }

    @Override
    public void assertCanCreateContent(Long userId) {
        User user = getExistingUser(userId);
        clearExpiredBanIfNecessary(user);

        if (user.getStatus() != UserStatus.ACTIVE || user.hasActiveLoginBan()) {
            throw new RuntimeException(buildBanMessage(user, "发布内容"));
        }
        if (user.hasActiveContentBan()) {
            throw new RuntimeException(buildBanMessage(user, "发表文章、问题或评论"));
        }
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
        AdminUserBanRequest request = new AdminUserBanRequest();
        request.setBanType(UserBanType.LOGIN.name());
        request.setReason(StringUtils.hasText(reason) ? reason : "管理员封禁");
        banUser(userId, request, BaseContext.getCurrentId());
    }

    @Override
    public void banUser(Long userId, AdminUserBanRequest request, Long adminId) {
        log.info("管理员封禁用户，用户ID: {}, 管理员ID: {}, 请求: {}", userId, adminId, request);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (userId.equals(adminId)) {
            throw new RuntimeException("不能封禁当前登录的账号");
        }
        if (user.isSuperAdmin()) {
            throw new RuntimeException("不能封禁超级管理员账号");
        }

        User admin = adminId == null ? null : userMapper.findById(adminId);
        UserBanType banType = parseBanType(request == null ? null : request.getBanType());
        if (banType == UserBanType.NONE) {
            throw new RuntimeException("请选择有效的封禁类型");
        }

        String reason = request == null ? "" : request.getReason();
        if (!StringUtils.hasText(reason)) {
            throw new RuntimeException("封禁理由不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer durationDays = request == null ? null : request.getDurationDays();
        if (durationDays != null && durationDays < 1) {
            throw new RuntimeException("封禁天数必须大于0");
        }
        user.setStatus(banType == UserBanType.LOGIN ? UserStatus.DISABLED : UserStatus.ACTIVE);
        user.setBanType(banType);
        user.setBanReason(reason.trim());
        user.setBanExpiresAt(durationDays == null ? null : now.plusDays(durationDays));
        user.setBannedAt(now);
        user.setBannedBy(adminId);
        user.setBannedByEmail(admin == null ? null : admin.getEmail());
        user.setUpdatedAt(now);

        int result = userMapper.updateModeration(user);
        if (result <= 0) {
            throw new RuntimeException("封禁用户失败");
        }

        log.info("用户封禁成功，用户ID: {}, 类型: {}", userId, banType);
    }

    @Override
    public void enableUser(Long userId) {
        unbanUser(userId);
    }

    @Override
    public void unbanUser(Long userId) {
        log.info("管理员解封用户，用户ID: {}", userId);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        clearBan(user);

        int result = userMapper.updateModeration(user);
        if (result <= 0) {
            throw new RuntimeException("解封用户失败");
        }

        log.info("用户解封成功，用户ID: {}", userId);
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

        if (userId.equals(BaseContext.getCurrentId()) && !UserRole.valueOf(role).equals(UserRole.ADMIN)
                && !UserRole.valueOf(role).equals(UserRole.SUPER_ADMIN)) {
            throw new RuntimeException("不能取消自己的管理员权限");
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

        if (userId.equals(BaseContext.getCurrentId())) {
            throw new RuntimeException("不能删除当前登录的账号");
        }

        if (user.isAdmin()) {
            throw new RuntimeException("不能直接删除管理员账号，请先将其降级为普通用户");
        }

        removeUserAsFollowTarget(userId);
        int result = userMapper.deleteById(userId);
        if (result <= 0) {
            throw new RuntimeException("删除用户失败");
        }

        log.info("用户删除成功，用户ID: {}", userId);
    }

    @Override
    public BatchOperationResult batchDisableUsers(List<Long> userIds, String reason) {
        return executeBatch(userIds, userId -> disableUser(userId, reason));
    }

    @Override
    public BatchOperationResult batchBanUsers(List<Long> userIds, AdminUserBanRequest request, Long adminId) {
        return executeBatch(userIds, userId -> banUser(userId, request, adminId));
    }

    @Override
    public BatchOperationResult batchEnableUsers(List<Long> userIds) {
        return executeBatch(userIds, this::enableUser);
    }

    @Override
    public BatchOperationResult batchDeleteUsers(List<Long> userIds) {
        return executeBatch(userIds, this::adminDeleteUser);
    }

    private BatchOperationResult executeBatch(List<Long> userIds, java.util.function.Consumer<Long> action) {
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("请选择要操作的用户");
        }

        BatchOperationResult result = new BatchOperationResult();
        result.setTotal(userIds.size());

        for (Long userId : userIds) {
            try {
                action.accept(userId);
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("用户 " + userId + "：" + e.getMessage());
            }
        }
        return result;
    }

    private void removeUserAsFollowTarget(Long userId) {
        followRepository.deleteByTargetTypeAndTargetId("USER", userId);
    }

    private void assertCanLogin(User user) {
        clearExpiredBanIfNecessary(user);
        if (user.getStatus() != UserStatus.ACTIVE || effectiveBanType(user) == UserBanType.LOGIN) {
            log.warn("用户登录失败，账户被封禁或状态异常: {}, 状态: {}, 封禁类型: {}",
                    user.getUsername(), user.getStatus(), effectiveBanType(user));
            throw new RuntimeException(buildBanMessage(user, "登录"));
        }
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void clearExpiredBanIfNecessary(User user) {
        if (user == null || effectiveBanType(user) == UserBanType.NONE || !user.isBanExpired()) {
            return;
        }

        clearBan(user);
        int result = userMapper.updateModeration(user);
        if (result <= 0) {
            throw new RuntimeException("封禁状态更新失败");
        }
        log.info("用户封禁已到期并自动解除，用户ID: {}", user.getId());
    }

    private void clearBan(User user) {
        user.setStatus(UserStatus.ACTIVE);
        user.setBanType(UserBanType.NONE);
        user.setBanReason(null);
        user.setBanExpiresAt(null);
        user.setBannedAt(null);
        user.setBannedBy(null);
        user.setBannedByEmail(null);
        user.setUpdatedAt(LocalDateTime.now());
    }

    private UserBanType effectiveBanType(User user) {
        if (user == null) {
            return UserBanType.NONE;
        }
        if (user.getBanType() != null && user.getBanType() != UserBanType.NONE) {
            return user.getBanType();
        }
        return user.getStatus() == UserStatus.ACTIVE ? UserBanType.NONE : UserBanType.LOGIN;
    }

    private UserBanType parseBanType(String banType) {
        try {
            return UserBanType.fromCode(banType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("封禁类型无效，仅支持 LOGIN 或 CONTENT");
        }
    }

    private String buildBanMessage(User user, String action) {
        UserBanType banType = effectiveBanType(user);
        String typeText = banType == UserBanType.CONTENT ? "内容封禁" : "登录封禁";
        String reason = StringUtils.hasText(user.getBanReason()) ? user.getBanReason() : "未记录";
        String adminEmail = StringUtils.hasText(user.getBannedByEmail()) ? user.getBannedByEmail() : "未记录";
        String expiresAt = user.getBanExpiresAt() == null
                ? "永久"
                : user.getBanExpiresAt().format(BAN_TIME_FORMATTER);
        return "账号已被" + typeText + "，暂不能" + action
                + "。封禁理由：" + reason
                + "；封禁期限：" + expiresAt
                + "；处理管理员：" + adminEmail;
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

    private User getExistingUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private TwoFactorService.PendingChallenge getLoginChallenge(String twoFactorToken) {
        TwoFactorService.PendingChallenge challenge = twoFactorService.getChallenge(
                twoFactorToken,
                TwoFactorService.ChallengeType.LOGIN);
        if (challenge == null) {
            throw new RuntimeException("两步验证已过期，请重新登录");
        }
        return challenge;
    }
}
