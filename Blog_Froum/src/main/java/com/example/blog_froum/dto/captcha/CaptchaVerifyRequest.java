package com.example.blog_froum.dto.captcha;

import lombok.Data;

@Data
public class CaptchaVerifyRequest {
    private String captchaId;
    private Float captchaPercentage;
}
