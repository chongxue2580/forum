package com.example.blog_froum.service;

import cloud.tianai.captcha.application.vo.CaptchaResponse;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;

public interface CaptchaService {
    CaptchaResponse<ImageCaptchaVO> generate();

    void validate(String captchaId, Float captchaPercentage);
}
