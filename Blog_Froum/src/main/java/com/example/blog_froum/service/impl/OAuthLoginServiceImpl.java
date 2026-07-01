package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.OAuthAuthorizeResponse;
import com.example.blog_froum.dto.user.OAuthCallbackRequest;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.UserRole;
import com.example.blog_froum.enums.UserStatus;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.service.OAuthLoginService;
import com.example.blog_froum.service.SystemSettingService;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.PasswordUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OAuthLoginServiceImpl implements OAuthLoginService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final String GOOGLE = "google";
    private static final String GITHUB = "github";

    @Value("${oauth.frontend-callback-url:http://localhost:5181/oauth/callback}")
    private String defaultFrontendCallbackUrl;

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.github.client-secret:}")
    private String githubClientSecret;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemSettingService systemSettingService;

    private final RestTemplate restTemplate = createRestTemplate();
    private final Map<String, PendingOAuthState> pendingStates = new ConcurrentHashMap<>();

    @Override
    public OAuthAuthorizeResponse buildAuthorizationUrl(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        OAuthProviderConfig config = getProviderConfig(normalizedProvider);
        assertProviderConfigured(normalizedProvider, config);
        cleanExpiredStates();

        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = getFrontendCallbackUrl();
        pendingStates.put(state, new PendingOAuthState(normalizedProvider, redirectUri, Instant.now().plus(STATE_TTL)));

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(config.getAuthorizationUrl())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", config.getScope())
                .queryParam("state", state);

        if (GOOGLE.equals(normalizedProvider)) {
            builder.queryParam("prompt", "select_account");
        }

        return new OAuthAuthorizeResponse(builder.build().encode().toUriString());
    }

    @Override
    @Transactional
    public LoginResponse completeLogin(String provider, OAuthCallbackRequest request) {
        String normalizedProvider = normalizeProvider(provider);
        if (request == null || !StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getState())) {
            throw new RuntimeException("第三方登录参数不完整");
        }

        PendingOAuthState state = consumeState(request.getState());
        if (state == null) {
            throw new RuntimeException("第三方登录状态已过期，请重新登录");
        }
        if (!normalizedProvider.equals(state.getProvider())) {
            throw new RuntimeException("第三方登录状态不匹配，请重新登录");
        }

        OAuthProviderConfig config = getProviderConfig(normalizedProvider);
        assertProviderConfigured(normalizedProvider, config);

        String accessToken = exchangeAccessToken(config, request.getCode(), state.getRedirectUri());
        OAuthProfile profile = fetchProfile(normalizedProvider, accessToken);
        if (!StringUtils.hasText(profile.getEmail()) || !Boolean.TRUE.equals(profile.getEmailVerified())) {
            throw new RuntimeException("第三方账号未提供已验证邮箱");
        }

        User user = resolveOrCreateUser(profile);
        userService.assertCanUseAccount(user.getId());
        return userService.completeLogin(user);
    }

    private User resolveOrCreateUser(OAuthProfile profile) {
        String email = normalizeEmail(profile.getEmail());
        User user = userMapper.findByOauthProviderAndSubject(profile.getProvider(), profile.getSubject());
        if (user != null) {
            return user;
        }

        user = userMapper.findByEmail(email);
        if (user != null) {
            bindOauthIdentityIfPossible(user, profile);
            return userMapper.findById(user.getId());
        }

        User newUser = new User();
        newUser.setUsername(generateUniqueUsername(profile));
        newUser.setEmail(email);
        newUser.setPassword(PasswordUtil.encode(UUID.randomUUID().toString() + UUID.randomUUID()));
        newUser.setNickname(truncate(firstText(profile.getName(), profile.getUsername(), newUser.getUsername()), 100));
        newUser.setAvatarUrl(profile.getAvatarUrl());
        newUser.setRole(UserRole.USER);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setLoginCount(0);
        newUser.setTwoFactorEnabled(false);
        newUser.setTwoFactorSecret(null);
        newUser.setOauthProvider(profile.getProvider());
        newUser.setOauthSubject(profile.getSubject());
        newUser.setOauthEmailVerified(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.insert(newUser);
        if (result <= 0) {
            throw new RuntimeException("第三方账号注册失败");
        }
        return newUser;
    }

    private void bindOauthIdentityIfPossible(User user, OAuthProfile profile) {
        if (StringUtils.hasText(user.getOauthProvider())
                && StringUtils.hasText(user.getOauthSubject())
                && (!profile.getProvider().equals(user.getOauthProvider())
                || !profile.getSubject().equals(user.getOauthSubject()))) {
            return;
        }

        user.setOauthProvider(profile.getProvider());
        user.setOauthSubject(profile.getSubject());
        user.setOauthEmailVerified(true);
        if (!StringUtils.hasText(user.getAvatarUrl()) && StringUtils.hasText(profile.getAvatarUrl())) {
            user.setAvatarUrl(profile.getAvatarUrl());
        } else {
            user.setAvatarUrl(null);
        }
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.updateOauthIdentity(user);
        if (result <= 0) {
            throw new RuntimeException("第三方账号绑定失败");
        }
    }

    private String exchangeAccessToken(OAuthProviderConfig config, String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    config.getTokenUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> data = response.getBody();
            Object accessToken = data == null ? null : data.get("access_token");
            if (!StringUtils.hasText(accessToken == null ? null : accessToken.toString())) {
                log.warn("第三方登录换取access token失败，响应: {}", data);
                throw new RuntimeException("第三方登录授权失败，请重新尝试");
            }
            return accessToken.toString();
        } catch (RestClientException e) {
            log.warn("第三方登录换取access token异常: {}", e.getMessage());
            throw new RuntimeException("第三方登录授权失败，请重新尝试");
        }
    }

    private OAuthProfile fetchProfile(String provider, String accessToken) {
        if (GOOGLE.equals(provider)) {
            return fetchGoogleProfile(accessToken);
        }
        if (GITHUB.equals(provider)) {
            return fetchGithubProfile(accessToken);
        }
        throw new RuntimeException("不支持的第三方登录方式");
    }

    private OAuthProfile fetchGoogleProfile(String accessToken) {
        Map<String, Object> userInfo = getJson(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                accessToken,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        OAuthProfile profile = new OAuthProfile();
        profile.setProvider(GOOGLE);
        profile.setSubject(asText(userInfo.get("sub")));
        profile.setEmail(normalizeEmail(asText(userInfo.get("email"))));
        profile.setEmailVerified(asBoolean(userInfo.get("email_verified")));
        profile.setName(asText(userInfo.get("name")));
        profile.setAvatarUrl(asText(userInfo.get("picture")));
        assertProfileSubject(profile);
        return profile;
    }

    private OAuthProfile fetchGithubProfile(String accessToken) {
        Map<String, Object> userInfo = getJson(
                "https://api.github.com/user",
                accessToken,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        GithubEmail verifiedEmail = fetchGithubVerifiedEmail(accessToken);

        OAuthProfile profile = new OAuthProfile();
        profile.setProvider(GITHUB);
        profile.setSubject(asText(userInfo.get("id")));
        profile.setUsername(asText(userInfo.get("login")));
        profile.setName(asText(userInfo.get("name")));
        profile.setAvatarUrl(asText(userInfo.get("avatar_url")));
        if (verifiedEmail != null) {
            profile.setEmail(normalizeEmail(verifiedEmail.getEmail()));
            profile.setEmailVerified(true);
        }
        assertProfileSubject(profile);
        return profile;
    }

    private GithubEmail fetchGithubVerifiedEmail(String accessToken) {
        List<Map<String, Object>> emails = getJson(
                "https://api.github.com/user/emails",
                accessToken,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });
        if (emails == null) {
            return null;
        }

        GithubEmail fallback = null;
        for (Map<String, Object> emailInfo : emails) {
            boolean verified = asBoolean(emailInfo.get("verified"));
            if (!verified) {
                continue;
            }

            GithubEmail email = new GithubEmail(asText(emailInfo.get("email")));
            if (!StringUtils.hasText(email.getEmail())) {
                continue;
            }
            if (asBoolean(emailInfo.get("primary"))) {
                return email;
            }
            if (fallback == null) {
                fallback = email;
            }
        }
        return fallback;
    }

    private <T> T getJson(String url, String accessToken, ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    responseType);
            return response.getBody();
        } catch (RestClientException e) {
            log.warn("第三方用户信息获取失败，URL: {}, 错误: {}", url, e.getMessage());
            throw new RuntimeException("第三方用户信息获取失败");
        }
    }

    private OAuthProviderConfig getProviderConfig(String provider) {
        Map<String, Object> oauthSettings = getOAuthSettings();

        if (GOOGLE.equals(provider)) {
            return new OAuthProviderConfig(
                    firstText(asText(oauthSettings.get("googleClientId")), googleClientId),
                    firstText(asText(oauthSettings.get("googleClientSecret")), googleClientSecret),
                    "https://accounts.google.com/o/oauth2/v2/auth",
                    "https://oauth2.googleapis.com/token",
                    "openid email profile");
        }
        if (GITHUB.equals(provider)) {
            return new OAuthProviderConfig(
                    firstText(asText(oauthSettings.get("githubClientId")), githubClientId),
                    firstText(asText(oauthSettings.get("githubClientSecret")), githubClientSecret),
                    "https://github.com/login/oauth/authorize",
                    "https://github.com/login/oauth/access_token",
                    "read:user user:email");
        }
        throw new RuntimeException("不支持的第三方登录方式");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOAuthSettings() {
        try {
            Object oauth = systemSettingService.getSettings().get("oauth");
            if (oauth instanceof Map) {
                return (Map<String, Object>) oauth;
            }
        } catch (Exception e) {
            log.warn("读取第三方登录系统设置失败，将使用环境变量配置: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private void assertProviderConfigured(String provider, OAuthProviderConfig config) {
        if (!StringUtils.hasText(config.getClientId()) || !StringUtils.hasText(config.getClientSecret())) {
            throw new RuntimeException(providerDisplayName(provider) + "登录未配置，请设置对应的Client ID和Client Secret");
        }
        if (!StringUtils.hasText(getFrontendCallbackUrl())) {
            throw new RuntimeException("第三方登录回调地址未配置");
        }
    }

    private String getFrontendCallbackUrl() {
        Map<String, Object> oauthSettings = getOAuthSettings();
        return firstText(asText(oauthSettings.get("frontendCallbackUrl")), defaultFrontendCallbackUrl);
    }

    private PendingOAuthState consumeState(String state) {
        cleanExpiredStates();
        if (!StringUtils.hasText(state)) {
            return null;
        }
        PendingOAuthState pendingState = pendingStates.remove(state);
        if (pendingState == null || pendingState.isExpired()) {
            return null;
        }
        return pendingState;
    }

    private void cleanExpiredStates() {
        pendingStates.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void assertProfileSubject(OAuthProfile profile) {
        if (!StringUtils.hasText(profile.getSubject())) {
            throw new RuntimeException("第三方账号未返回唯一ID");
        }
    }

    private String generateUniqueUsername(OAuthProfile profile) {
        String base = firstText(profile.getUsername(), profile.getName(), emailPrefix(profile.getEmail()), profile.getProvider() + "_user");
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        base = trimUnderscores(base);
        if (base.length() < 3) {
            base = profile.getProvider() + "_" + base;
        }
        base = truncate(base, 42);

        String candidate = base;
        for (int i = 0; i < 1000; i++) {
            if (!userMapper.existsByUsername(candidate)) {
                return candidate;
            }
            candidate = truncate(base, 42) + "_" + (i + 1);
        }
        return truncate(profile.getProvider() + "_" + UUID.randomUUID().toString().replace("-", ""), 50);
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!GOOGLE.equals(normalized) && !GITHUB.equals(normalized)) {
            throw new RuntimeException("不支持的第三方登录方式");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String emailPrefix(String email) {
        String normalizedEmail = normalizeEmail(email);
        int index = normalizedEmail.indexOf('@');
        return index > 0 ? normalizedEmail.substring(0, index) : normalizedEmail;
    }

    private String trimUnderscores(String value) {
        String result = value;
        while (result.startsWith("_")) {
            result = result.substring(1);
        }
        while (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private String providerDisplayName(String provider) {
        return GOOGLE.equals(provider) ? "Google" : "GitHub";
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    @Data
    private static class PendingOAuthState {
        private final String provider;
        private final String redirectUri;
        private final Instant expiresAt;

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Data
    private static class OAuthProviderConfig {
        private final String clientId;
        private final String clientSecret;
        private final String authorizationUrl;
        private final String tokenUrl;
        private final String scope;
    }

    @Data
    private static class OAuthProfile {
        private String provider;
        private String subject;
        private String email;
        private Boolean emailVerified;
        private String username;
        private String name;
        private String avatarUrl;
    }

    @Data
    private static class GithubEmail {
        private final String email;
    }
}
