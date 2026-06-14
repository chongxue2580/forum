package com.example.blog_froum.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.CaptchaResponse;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import com.example.blog_froum.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;

    @Value("${security.tac.enabled:true}")
    private boolean enabled;

    @Override
    public CaptchaResponse<ImageCaptchaVO> generate() {
        return imageCaptchaApplication.generateCaptcha();
    }

    @Override
    public void validate(String captchaId, Float captchaPercentage) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(captchaId) || captchaPercentage == null) {
            throw new RuntimeException("请先完成验证码验证");
        }
        boolean matched = imageCaptchaApplication.matching(captchaId, captchaPercentage);
        if (!matched) {
            log.warn("TAC验证码校验失败，captchaId={}", captchaId);
            throw new RuntimeException("验证码验证失败，请重试");
        }
    }
}
