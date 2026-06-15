package com.example.blog_froum.interceptor;

import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.JwtUtil;
import com.example.blog_froum.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT用户拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

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

        // 检查是否是需要认证的请求
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 对于文章、评论、问答的写操作需要认证
        if (needsAuthentication(request, requestURI, method)) {
            // 需要认证，继续执行JWT验证
        } else {
            // 不需要强制认证的接口，如果请求带了有效 token，也写入当前用户上下文，便于返回用户态信息。
            resolveOptionalUserContext(request);
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
            log.info("用户JWT校验: {}", token);
            
            if (!StringUtils.hasText(token)) {
                log.info("用户JWT令牌为空，返回未登录信息");
                response.setStatus(401);
                return false;
            }

            // 验证token
            if (!jwtUtil.validateToken(token)) {
                log.info("用户JWT令牌无效，返回未登录信息");
                response.setStatus(401);
                return false;
            }

            // 获取用户信息并存储到请求中
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            try {
                userService.assertCanUseAccount(userId);
            } catch (RuntimeException e) {
                log.warn("用户账号已被限制访问，用户ID: {}, 原因: {}", userId, e.getMessage());
                writeBusinessError(response, 403, e.getMessage());
                return false;
            }

            log.info("用户JWT校验成功，用户ID: {}, 用户名: {}, 角色: {}", userId, username, role);
            
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
            log.error("用户JWT校验失败: {}", ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    /**
     * 判断请求是否需要认证
     */
    private boolean needsAuthentication(HttpServletRequest request, String requestURI, String method) {
        // 文章相关的写操作需要认证
        if (requestURI.startsWith("/api/articles")) {
            return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
        }

        // 评论相关的写操作需要认证
        if (requestURI.startsWith("/api/comments")) {
            return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
        }

        // 问答相关的写操作需要认证
        if (requestURI.startsWith("/api/questions")) {
            return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
        }

        // 用户相关接口
        if (requestURI.startsWith("/api/user")) {
            if (isPublicUserEndpoint(requestURI, method)) {
                return false;
            }

            // 获取用户资料和统计数据不需要认证
            if (requestURI.matches("/api/user/profile/\\d+") && "GET".equals(method)) {
                return false;
            }
            if (requestURI.matches("/api/user/profile/\\d+/stats") && "GET".equals(method)) {
                return false;
            }
            // 获取用户问答和文章列表不需要认证
            if (requestURI.matches("/api/user/profile/\\d+/questions") && "GET".equals(method)) {
                return false;
            }
            if (requestURI.matches("/api/user/profile/\\d+/articles") && "GET".equals(method)) {
                return false;
            }
            if ("/api/user/recent-users".equals(requestURI) && "GET".equals(method)) {
                return false;
            }
            // 其他用户接口需要认证
            return true;
        }

        if ("GET".equals(method)
                && ("/api/follows/users".equals(requestURI) || "/api/follows/followers".equals(requestURI))
                && StringUtils.hasText(request.getParameter("userId"))) {
            return false;
        }

        if ("GET".equals(method) && requestURI.matches("/api/follows/[^/]+/\\d+/(status|count|info)")) {
            return false;
        }

        if ("GET".equals(method) && requestURI.matches("/api/likes/[^/]+/\\d+/(status|count|info)")) {
            return false;
        }

        // 关注相关接口都需要认证
        if (requestURI.startsWith("/api/follows")) {
            return true;
        }

        // 点赞相关接口都需要认证
        if (requestURI.startsWith("/api/likes")) {
            return true;
        }

        // 其他路径默认需要认证（如果被拦截器拦截到的话）
        return true;
    }

    private boolean isPublicUserEndpoint(String requestURI, String method) {
        if ("POST".equals(method)
                && ("/api/user/login".equals(requestURI)
                || "/api/user/register".equals(requestURI)
                || "/api/user/register/email-code".equals(requestURI)
                || "/api/user/forgot-password/email-code".equals(requestURI)
                || "/api/user/forgot-password/reset".equals(requestURI)
                || "/api/user/profile/login".equals(requestURI)
                || "/api/user/profile/register".equals(requestURI))) {
            return true;
        }

        return "GET".equals(method)
                && ("/api/user/check-username".equals(requestURI)
                || "/api/user/check-email".equals(requestURI));
    }

    private void resolveOptionalUserContext(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token)) {
            return;
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                return;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            BaseContext.setCurrentId(userId);
        } catch (Exception ex) {
            log.debug("可选用户JWT解析失败，按未登录访问处理: {}", ex.getMessage());
        }
    }

    private void writeBusinessError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\""
                + escapeJson(message)
                + "\",\"data\":null,\"code\":"
                + status
                + "}");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
