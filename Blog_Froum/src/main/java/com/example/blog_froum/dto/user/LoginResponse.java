package com.example.blog_froum.dto.user;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    private String token;
    private UserResponse user;
    private Boolean requiresTwoFactor = false;
    private String twoFactorToken;
    private Boolean requiresTwoFactorSetup = false;
    private String setupToken;
    private String twoFactorSecret;
    private String twoFactorOtpAuthUrl;

    public LoginResponse() {}

    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public static LoginResponse requiresTwoFactor(String twoFactorToken, UserResponse user) {
        LoginResponse response = new LoginResponse();
        response.setRequiresTwoFactor(true);
        response.setTwoFactorToken(twoFactorToken);
        response.setUser(user);
        return response;
    }

    public static LoginResponse requiresTwoFactorSetup(
            String setupToken,
            String secret,
            String otpAuthUrl,
            UserResponse user) {
        LoginResponse response = new LoginResponse();
        response.setRequiresTwoFactorSetup(true);
        response.setSetupToken(setupToken);
        response.setTwoFactorSecret(secret);
        response.setTwoFactorOtpAuthUrl(otpAuthUrl);
        response.setUser(user);
        return response;
    }
}
