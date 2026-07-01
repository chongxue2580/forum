package com.example.blog_froum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf().disable()

            // 配置CORS
            .cors().configurationSource(corsConfigurationSource())

            .and()

            // 配置会话管理为无状态
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            .and()

            // 配置请求授权 - 现在通过JWT拦截器来处理认证
            .authorizeHttpRequests(authz -> authz
                // 允许Swagger相关资源
                .antMatchers("/doc.html", "/webjars/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers("/api/captcha/**").permitAll()
                // 允许登录注册接口
                .antMatchers("/api/user/login", "/api/user/register").permitAll()
                .antMatchers("/api/user/register/email-code").permitAll()
                .antMatchers("/api/user/oauth/*/authorize", "/api/user/oauth/*/callback").permitAll()
                .antMatchers("/api/user/forgot-password/email-code", "/api/user/forgot-password/reset").permitAll()
                .antMatchers("/api/user/profile/login", "/api/user/profile/register").permitAll()
                .antMatchers("/api/user/check-username", "/api/user/check-email").permitAll()
                .antMatchers("/api/admin/login").permitAll()
                .antMatchers("/api/admin/2fa/email-code").permitAll()
                .antMatchers("/api/admin/2fa/setup/confirm").permitAll()
                // 其他请求都允许通过，由JWT拦截器处理
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5174",  // 添加5174端口
            "http://localhost:5175",  // 添加5175端口
            "http://localhost:5177",  // 添加5177端口
            "http://localhost:5178",  // 添加5178端口
            "http://localhost:5179",  // 添加5179端口
            "http://localhost:5180",  // 添加5180端口
            "http://localhost:5181",  // 添加5181端口
            "http://localhost:5182",  // 添加5182端口
            "http://localhost:5183",  // 添加5183端口
            "http://localhost:5184",  // 添加5184端口
            "http://localhost:5185",  // 添加5185端口
            "http://localhost:5190",  // 添加5190端口
            "http://localhost:8081"
        ));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许发送凭据
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
