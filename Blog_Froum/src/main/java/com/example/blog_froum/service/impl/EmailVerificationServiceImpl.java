package com.example.blog_froum.service.impl;

import com.example.blog_froum.entity.User;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.service.EmailVerificationService;
import com.example.blog_froum.service.SystemSettingService;
import com.example.blog_froum.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final String PURPOSE_REGISTER = "register";
    private static final String PURPOSE_EMAIL_CHANGE = "email-change";
    private static final String PURPOSE_PASSWORD_CHANGE = "password-change";
    private static final String PURPOSE_ADMIN_TWO_FACTOR = "admin-2fa-login";
    private static final String PURPOSE_PASSWORD_RESET = "password-reset";
    private static final String CODE_PLACEHOLDER = "${code}";

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private UserMapper userMapper;

    @Value("${brevo.smtp.host:smtp-relay.brevo.com}")
    private String defaultSmtpHost;

    @Value("${brevo.smtp.port:587}")
    private int defaultSmtpPort;

    @Value("${brevo.smtp.username:}")
    private String defaultSmtpUsername;

    @Value("${brevo.smtp.password:}")
    private String defaultSmtpPassword;

    @Value("${brevo.smtp.sender-email:}")
    private String defaultSenderEmail;

    @Value("${brevo.smtp.sender-name:FroumX 社区}")
    private String defaultSenderName;

    @Override
    public void sendRegistrationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userMapper.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("邮箱已存在");
        }

        sendCode(normalizedEmail, PURPOSE_REGISTER, "注册邮箱验证", buildRegisterEmailHtml(CODE_PLACEHOLDER));
    }

    @Override
    public void verifyRegistrationCode(String email, String code) {
        verifyCode(normalizeEmail(email), PURPOSE_REGISTER, code);
    }

    @Override
    public void sendEmailChangeCode(Long userId, String newEmail) {
        User user = findUserById(userId);
        String normalizedEmail = normalizeEmail(newEmail);
        String currentEmail = normalizeEmail(user.getEmail());

        if (normalizedEmail.equals(currentEmail)) {
            throw new RuntimeException("新邮箱不能与当前邮箱相同");
        }
        if (userMapper.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("邮箱已被使用");
        }

        sendCode(normalizedEmail, PURPOSE_EMAIL_CHANGE, "更换邮箱验证", buildEmailChangeEmailHtml(CODE_PLACEHOLDER));
    }

    @Override
    public void verifyEmailChangeCode(String newEmail, String code) {
        verifyCode(normalizeEmail(newEmail), PURPOSE_EMAIL_CHANGE, code);
    }

    @Override
    public void sendPasswordChangeCode(Long userId) {
        User user = findUserById(userId);
        String email = normalizeEmail(user.getEmail());
        sendCode(email, PURPOSE_PASSWORD_CHANGE, "修改密码验证", buildPasswordChangeEmailHtml(CODE_PLACEHOLDER));
    }

    @Override
    public void verifyPasswordChangeCode(Long userId, String code) {
        User user = findUserById(userId);
        verifyCode(normalizeEmail(user.getEmail()), PURPOSE_PASSWORD_CHANGE, code);
    }

    @Override
    public void sendAdminTwoFactorCode(Long userId) {
        User user = findUserById(userId);
        if (!user.isAdmin()) {
            throw new RuntimeException("权限不足，非管理员账户");
        }

        sendCode(normalizeEmail(user.getEmail()), PURPOSE_ADMIN_TWO_FACTOR, "管理员登录验证", buildAdminTwoFactorEmailHtml(CODE_PLACEHOLDER));
    }

    @Override
    public void verifyAdminTwoFactorCode(Long userId, String code) {
        User user = findUserById(userId);
        if (!user.isAdmin()) {
            throw new RuntimeException("权限不足，非管理员账户");
        }

        verifyCode(normalizeEmail(user.getEmail()), PURPOSE_ADMIN_TWO_FACTOR, code);
    }

    @Override
    public void sendPasswordResetCode(String account) {
        User user = findUserByAccount(account);
        sendCode(user.getEmail(), PURPOSE_PASSWORD_RESET, "重置密码验证", buildPasswordResetEmailHtml(CODE_PLACEHOLDER));
    }

    @Override
    public void resetPassword(String account, String code, String newPassword) {
        User user = findUserByAccount(account);
        verifyCode(user.getEmail(), PURPOSE_PASSWORD_RESET, code);

        int result = userMapper.updatePassword(user.getId(), PasswordUtil.encode(newPassword));
        if (result <= 0) {
            throw new RuntimeException("重置密码失败");
        }

        log.info("用户忘记密码重置成功，用户ID: {}", user.getId());
    }

    private void sendCode(String email, String purpose, String subject, String htmlTemplate) {
        ensureCooldown(email, purpose);

        String code = generateCode();
        String codeKey = codeKey(purpose, email);
        String cooldownKey = cooldownKey(purpose, email);

        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", SEND_COOLDOWN);

        try {
            String html = htmlTemplate.replace("${code}", code);
            sendHtmlEmail(email, subject, html);
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(cooldownKey);
            throw e;
        }
    }

    private void ensureCooldown(String email, String purpose) {
        Boolean coolingDown = stringRedisTemplate.hasKey(cooldownKey(purpose, email));
        if (Boolean.TRUE.equals(coolingDown)) {
            throw new RuntimeException("验证码发送过于频繁，请60秒后再试");
        }
    }

    private void verifyCode(String email, String purpose, String code) {
        String normalizedCode = code == null ? "" : code.trim();
        String key = codeKey(purpose, email);
        String storedCode = stringRedisTemplate.opsForValue().get(key);

        if (!StringUtils.hasText(storedCode)) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(normalizedCode)) {
            throw new RuntimeException("验证码错误");
        }

        stringRedisTemplate.delete(key);
    }

    private User findUserByAccount(String account) {
        String normalizedAccount = account == null ? "" : account.trim();
        if (!StringUtils.hasText(normalizedAccount)) {
            throw new RuntimeException("用户名或邮箱不能为空");
        }

        User user = userMapper.findByUsernameOrEmail(normalizedAccount, normalizedAccount);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new RuntimeException("该账号未绑定邮箱");
        }
        return user;
    }

    private User findUserById(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new RuntimeException("该账号未绑定邮箱");
        }
        return user;
    }

    private void sendHtmlEmail(String recipient, String subject, String html) {
        MailConfig config = resolveMailConfig();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.host);
        mailSender.setPort(config.port);
        mailSender.setUsername(config.username);
        mailSender.setPassword(config.password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", String.valueOf(config.enableSsl));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.enableStartTls));
        props.put("mail.smtp.starttls.required", String.valueOf(config.enableStartTls));
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(config.senderEmail, config.senderName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailAuthenticationException e) {
            log.error("邮箱验证码发送认证失败，收件人: {}", recipient, e);
            throw new RuntimeException("SMTP账号或授权码错误，请检查邮件设置");
        } catch (Exception e) {
            log.error("邮箱验证码发送失败，收件人: {}", recipient, e);
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @SuppressWarnings("unchecked")
    private MailConfig resolveMailConfig() {
        Map<String, Object> settings = systemSettingService.getSettings();
        Object emailGroupValue = settings.get("email");
        Map<String, Object> email = emailGroupValue instanceof Map
                ? (Map<String, Object>) emailGroupValue
                : Collections.emptyMap();

        String configuredHost = asString(email.get("smtpServer"));
        String host = StringUtils.hasText(configuredHost) ? configuredHost : defaultSmtpHost;
        int port = StringUtils.hasText(configuredHost)
                ? asInt(email.get("smtpPort"), defaultSmtpPort)
                : defaultSmtpPort;
        String username = firstText(asString(email.get("smtpUsername")), defaultSmtpUsername);
        String password = firstText(asString(email.get("smtpPassword")), defaultSmtpPassword);
        String senderEmail = firstText(asString(email.get("senderEmail")), defaultSenderEmail);
        String senderName = firstText(asString(email.get("senderName")), defaultSenderName);
        boolean tlsEnabled = asBoolean(email.get("enableSsl"), true);

        if (!StringUtils.hasText(host)) {
            throw new RuntimeException("SMTP服务器未配置");
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new RuntimeException("SMTP账号或密钥未配置");
        }
        if (!StringUtils.hasText(senderEmail)) {
            throw new RuntimeException("发件人邮箱未配置");
        }

        MailConfig config = new MailConfig();
        config.host = host;
        config.port = port;
        config.username = username;
        config.password = password;
        config.senderEmail = senderEmail;
        config.senderName = StringUtils.hasText(senderName) ? senderName : "FroumX 社区";
        config.enableSsl = tlsEnabled && port == 465;
        config.enableStartTls = tlsEnabled && port != 465;
        return config;
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new RuntimeException("邮箱不能为空");
        }
        return normalizedEmail;
    }

    private String codeKey(String purpose, String email) {
        return "froumx:email-code:" + purpose + ":" + email;
    }

    private String cooldownKey(String purpose, String email) {
        return "froumx:email-code:cooldown:" + purpose + ":" + email;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int asInt(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String buildRegisterEmailHtml(String codePlaceholder) {
        return buildEmailHtml("注册邮箱验证", "请输入以下验证码完成账号注册。", codePlaceholder);
    }

    private String buildEmailChangeEmailHtml(String codePlaceholder) {
        return buildEmailHtml("更换邮箱验证", "请输入以下验证码确认绑定该邮箱。", codePlaceholder);
    }

    private String buildPasswordChangeEmailHtml(String codePlaceholder) {
        return buildEmailHtml("修改密码验证", "请输入以下验证码确认本次密码修改。", codePlaceholder);
    }

    private String buildAdminTwoFactorEmailHtml(String codePlaceholder) {
        return buildEmailHtml("管理员登录验证", "请输入以下验证码完成本次管理员登录。", codePlaceholder);
    }

    private String buildPasswordResetEmailHtml(String codePlaceholder) {
        return buildEmailHtml("重置密码验证", "请输入以下验证码重置账号密码。", codePlaceholder);
    }

    private String buildEmailHtml(String title, String description, String codePlaceholder) {
        return "<!doctype html>"
                + "<html><body style=\"margin:0;background:#f5f7fb;font-family:Arial,'Microsoft YaHei',sans-serif;color:#1f2937;\">"
                + "<div style=\"max-width:560px;margin:0 auto;padding:32px 16px;\">"
                + "<div style=\"background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;padding:28px;box-shadow:0 10px 30px rgba(15,23,42,.08);\">"
                + "<h2 style=\"margin:0 0 12px;font-size:22px;color:#111827;\">FroumX " + title + "</h2>"
                + "<p style=\"margin:0 0 20px;font-size:15px;line-height:1.7;color:#4b5563;\">" + description + "</p>"
                + "<div style=\"letter-spacing:8px;font-size:32px;font-weight:700;text-align:center;color:#2563eb;background:#eff6ff;border-radius:10px;padding:18px 12px;margin:20px 0;\">"
                + codePlaceholder
                + "</div>"
                + "<p style=\"margin:0;font-size:13px;line-height:1.7;color:#6b7280;\">验证码5分钟内有效，且只能使用一次。若不是你本人操作，请忽略本邮件。</p>"
                + "</div>"
                + "</div>"
                + "</body></html>";
    }

    private static class MailConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private String senderEmail;
        private String senderName;
        private boolean enableSsl;
        private boolean enableStartTls;
    }
}
