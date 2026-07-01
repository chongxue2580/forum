package com.example.blog_froum.service;

import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.OAuthAuthorizeResponse;
import com.example.blog_froum.dto.user.OAuthCallbackRequest;

public interface OAuthLoginService {
    OAuthAuthorizeResponse buildAuthorizationUrl(String provider);

    LoginResponse completeLogin(String provider, OAuthCallbackRequest request);
}
