package com.example.blog_froum.service.impl;

import com.example.blog_froum.entity.SystemSetting;
import com.example.blog_froum.repository.SystemSettingRepository;
import com.example.blog_froum.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@Transactional
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final LinkedHashMap<String, Object> DEFAULT_VALUES = new LinkedHashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>();

    static {
        define("site.name", "FroumX 技术社区", "站点名称", false);
        define("site.description", "专注于前端开发、后端开发、移动开发、人工智能等技术交流的社区", "站点描述", false);
        define("site.logo", "/logo.png", "站点Logo", false);
        define("site.favicon", "/favicon.ico", "站点Favicon", false);
        define("site.icp", "", "ICP备案号", false);
        define("site.allowRegistration", true, "允许注册", false);
        define("site.requireEmailVerification", false, "邮箱验证", false);
        define("site.defaultUserRole", "USER", "默认用户角色", false);

        define("content.articleReviewEnabled", true, "文章审核", false);
        define("content.questionReviewEnabled", false, "问答审核", false);
        define("content.commentReviewEnabled", false, "评论审核", false);
        define("content.allowAnonymousView", true, "允许匿名访问", false);
        define("content.allowGuestComment", false, "允许游客评论", false);
        define("content.sensitiveWords", "", "敏感词", false);
        define("content.maxUploadSize", 10, "最大上传大小", false);
        define("content.allowedFileTypes", "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar", "允许的文件类型", false);

        define("email.smtpServer", "smtp-relay.brevo.com", "SMTP服务器", false);
        define("email.smtpPort", 587, "SMTP端口", false);
        define("email.smtpUsername", "", "SMTP用户名", false);
        define("email.smtpPassword", "", "SMTP密码", true);
        define("email.senderName", "FroumX 社区", "发件人名称", false);
        define("email.senderEmail", "", "发件人邮箱", false);
        define("email.enableSsl", true, "启用SSL", false);

        define("oauth.frontendCallbackUrl", "", "第三方登录前端回调地址", false);
        define("oauth.githubClientId", "", "GitHub Client ID", false);
        define("oauth.githubClientSecret", "", "GitHub Client Secret", true);
        define("oauth.googleClientId", "", "Google Client ID", false);
        define("oauth.googleClientSecret", "", "Google Client Secret", true);

        define("security.jwtExpireDays", 7, "JWT过期天数", false);
        define("security.maxLoginAttempts", 5, "最大登录尝试次数", false);
        define("security.lockoutMinutes", 30, "锁定时间", false);
        define("security.enableRecaptcha", false, "启用reCAPTCHA", false);
        define("security.recaptchaSiteKey", "", "reCAPTCHA Site Key", false);
        define("security.recaptchaSecretKey", "", "reCAPTCHA Secret Key", true);

        define("optimization.enableCache", false, "启用缓存", false);
        define("optimization.cacheTtl", 3600, "缓存过期时间", false);
        define("optimization.pageSize", 20, "默认分页大小", false);
        define("optimization.maxSearchResults", 100, "最大搜索结果数", false);
    }

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Override
    public Map<String, Object> getSettings() {
        ensureDefaultRows();
        LinkedHashMap<String, Object> flatSettings = new LinkedHashMap<>(DEFAULT_VALUES);
        for (SystemSetting setting : systemSettingRepository.findAll()) {
            flatSettings.put(setting.getSettingKey(), parseValue(setting.getSettingValue(), setting.getValueType()));
        }
        return unflatten(flatSettings);
    }

    @Override
    public Map<String, Object> updateSettings(Map<String, Object> settings) {
        if (settings == null) {
            throw new IllegalArgumentException("设置内容不能为空");
        }

        ensureDefaultRows();
        Map<String, Object> flattened = new LinkedHashMap<>();
        flatten("", settings, flattened);

        for (Map.Entry<String, Object> entry : flattened.entrySet()) {
            String key = entry.getKey();
            if (!DEFAULT_VALUES.containsKey(key)) {
                continue;
            }

            Object value = entry.getValue();
            if (SENSITIVE_KEYS.contains(key) && "********".equals(value)) {
                continue;
            }
            saveSetting(key, value);
        }

        return getSettings();
    }

    @Override
    public Map<String, Object> resetToDefaults() {
        systemSettingRepository.deleteAll();
        ensureDefaultRows();
        return getSettings();
    }

    @Override
    public void sendTestEmail(String recipient) {
        Map<String, Object> settings = getSettings();
        Map<String, Object> email = getGroup(settings, "email");

        String host = asString(email.get("smtpServer"));
        int port = asInt(email.get("smtpPort"), 587);
        String username = asString(email.get("smtpUsername"));
        String password = asString(email.get("smtpPassword"));
        String senderEmail = asString(email.get("senderEmail"));
        String senderName = asString(email.get("senderName"));
        boolean tlsEnabled = asBoolean(email.get("enableSsl"), true);
        boolean enableSsl = tlsEnabled && port == 465;
        boolean enableStartTls = tlsEnabled && port != 465;
        String target = StringUtils.hasText(recipient) ? recipient : senderEmail;

        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException("SMTP服务器未配置");
        }
        if (!StringUtils.hasText(senderEmail)) {
            throw new IllegalStateException("发件人邮箱未配置");
        }
        if (!StringUtils.hasText(target)) {
            throw new IllegalStateException("测试收件人未配置");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        if (StringUtils.hasText(username)) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(username)));
        props.put("mail.smtp.ssl.enable", String.valueOf(enableSsl));
        props.put("mail.smtp.starttls.enable", String.valueOf(enableStartTls));
        props.put("mail.smtp.starttls.required", String.valueOf(enableStartTls));
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(target);
        message.setSubject("FroumX 系统邮件测试");
        message.setText("这是一封来自 " + (StringUtils.hasText(senderName) ? senderName : "FroumX") + " 的系统设置测试邮件。");

        mailSender.send(message);
    }

    @Override
    public int clearCache() {
        if (cacheManager == null || cacheManager.getCacheNames().isEmpty()) {
            throw new IllegalStateException("当前后端未启用缓存管理器");
        }

        int cleared = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                cleared++;
            }
        }
        return cleared;
    }

    private void ensureDefaultRows() {
        for (Map.Entry<String, Object> entry : DEFAULT_VALUES.entrySet()) {
            String key = entry.getKey();
            if (!systemSettingRepository.findBySettingKey(key).isPresent()) {
                saveSetting(key, entry.getValue());
            }
        }
    }

    private void saveSetting(String key, Object value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key).orElseGet(SystemSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value == null ? "" : String.valueOf(value));
        setting.setSettingGroup(key.contains(".") ? key.substring(0, key.indexOf('.')) : "general");
        setting.setValueType(resolveValueType(DEFAULT_VALUES.get(key)));
        setting.setDescription(DESCRIPTIONS.get(key));
        setting.setIsSensitive(SENSITIVE_KEYS.contains(key));
        systemSettingRepository.save(setting);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGroup(Map<String, Object> settings, String group) {
        Object value = settings.get(group);
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten(key, (Map<String, Object>) value, target);
            } else {
                target.put(key, value);
            }
        }
    }

    private Map<String, Object> unflatten(Map<String, Object> flatSettings) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flatSettings.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            Map<String, Object> current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.computeIfAbsent(parts[i], ignored -> new LinkedHashMap<String, Object>());
                if (next instanceof Map) {
                    current = castMap(next);
                }
            }
            current.put(parts[parts.length - 1], entry.getValue());
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Object parseValue(String value, String type) {
        if ("BOOLEAN".equalsIgnoreCase(type)) {
            return Boolean.parseBoolean(value);
        }
        if ("NUMBER".equalsIgnoreCase(type)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return value == null ? "" : value;
    }

    private String resolveValueType(Object value) {
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        if (value instanceof Number) {
            return "NUMBER";
        }
        return "STRING";
    }

    private static void define(String key, Object value, String description, boolean sensitive) {
        DEFAULT_VALUES.put(key, value);
        DESCRIPTIONS.put(key, description);
        if (sensitive) {
            SENSITIVE_KEYS.add(key);
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(asString(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
