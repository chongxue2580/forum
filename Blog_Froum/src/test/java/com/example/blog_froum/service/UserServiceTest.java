package com.example.blog_froum.service;

import com.example.blog_froum.dto.user.LoginRequest;
import com.example.blog_froum.dto.user.LoginResponse;
import com.example.blog_froum.dto.user.RegisterRequest;
import com.example.blog_froum.dto.user.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testRegister() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("123456");
        request.setNickname("测试用户");

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("测试用户", response.getNickname());
    }

    @Test
    public void testLogin() {
        // 先注册一个用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("logintest");
        registerRequest.setEmail("logintest@example.com");
        registerRequest.setPassword("123456");
        
        userService.register(registerRequest);

        // 然后测试登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("logintest");
        loginRequest.setPassword("123456");

        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUser());
        assertEquals("logintest", response.getUser().getUsername());
    }

    @Test
    public void testLoginWithWrongPassword() {
        // 先注册一个用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("wrongpasstest");
        registerRequest.setEmail("wrongpasstest@example.com");
        registerRequest.setPassword("123456");
        
        userService.register(registerRequest);

        // 然后测试用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("wrongpasstest");
        loginRequest.setPassword("wrongpassword");

        assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    public void testRegisterDuplicateUsername() {
        RegisterRequest request1 = new RegisterRequest();
        request1.setUsername("duplicate");
        request1.setEmail("duplicate1@example.com");
        request1.setPassword("123456");

        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("duplicate");
        request2.setEmail("duplicate2@example.com");
        request2.setPassword("123456");

        userService.register(request1);

        assertThrows(RuntimeException.class, () -> {
            userService.register(request2);
        });
    }
}
