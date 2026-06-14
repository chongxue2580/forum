package com.example.blog_froum.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaptchaResourceConfiguration {

    @Bean
    public ApplicationRunner tianaiCaptchaResourceInitializer(ResourceStore resourceStore) {
        return args -> {
            Resource builtInBackground = new Resource("classpath", "META-INF/cut-image/resource/1.jpg");
            resourceStore.addResource(CaptchaTypeConstant.SLIDER, builtInBackground);
            resourceStore.addResource(CaptchaTypeConstant.ROTATE, builtInBackground);
        };
    }
}
