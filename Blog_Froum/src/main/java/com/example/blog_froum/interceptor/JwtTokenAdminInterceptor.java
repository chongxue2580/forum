package com.example.blog_froum.interceptor;

import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT管理员拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 校验JWT
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader("Authorization");
        
        // 如果token以"Bearer "开头，去掉前缀
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 2、校验令牌
        try {
            log.info("管理员JWT校验: {}", token);
            
            if (!StringUtils.hasText(token)) {
                log.info("管理员JWT令牌为空，返回未登录信息");
                response.setStatus(401);
                return false;
            }

            // 验证token
            if (!jwtUtil.validateToken(token)) {
                log.info("管理员JWT令牌无效，返回未登录信息");
                response.setStatus(401);
                return false;
            }

            // 获取用户角色
            String role = jwtUtil.getRoleFromToken(token);
            
            // 检查是否为管理员角色
            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                log.info("用户角色不是管理员，角色: {}", role);
                response.setStatus(403);
                return false;
            }

            // 获取用户信息并存储到请求中
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            
            log.info("管理员JWT校验成功，用户ID: {}, 用户名: {}, 角色: {}", userId, username, role);
            
            // 将用户信息存储到请求属性中，供Controller使用
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);

            // 将用户ID存储到ThreadLocal中
            BaseContext.setCurrentId(userId);

            // 3、通过，放行
            return true;
        } catch (Exception ex) {
            // 4、不通过，响应401状态码
            log.error("管理员JWT校验失败: {}", ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }
    
    /**
     * 请求完成后执行
     * 用于清理ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 在请求结束后，清除ThreadLocal中的用户ID
        BaseContext.removeCurrentId();
        log.debug("已清除ThreadLocal中的用户ID");
    }
}
