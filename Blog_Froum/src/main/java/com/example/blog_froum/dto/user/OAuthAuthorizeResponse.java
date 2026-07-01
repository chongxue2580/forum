package com.example.blog_froum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAuthorizeResponse {
    private String authorizationUrl;
}
