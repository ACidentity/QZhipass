package org.microsoft.qintelipass.interceptors;

import org.microsoft.qintelipass.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserStatusInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 跳过不需要认证的接口
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            return true;
        }

        // 2. 从请求中获取用户标识（这里需要根据你的实际认证方式调整）
        // 示例：从Authorization header中获取userId
        String userId = extractUserId(request);
        
        if (userId != null) {
            // 3. 检查用户是否已停用
            if (userService.isUserDeactivated(userId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Your account has been deactivated\",\"code\":\"USER_DEACTIVATED\"}");
                return false; // 拦截请求
            }
        }

        return true; // 放行
    }

    /**
     * 判断是否需要跳过拦截
     */
    private boolean shouldSkip(String path) {
        // 跳过登录接口、静态资源等
        return path.contains("/login") 
            || path.contains("/register")
            || path.contains("/static/")
            || path.contains("/error");
    }

    /**
     * 从请求中提取用户ID
     * 方式：从 X-User-Id Header中获取（前端在登录后保存并传递）
     */
    private String extractUserId(HttpServletRequest request) {
        // 从自定义Header中获取用户ID
        String userId = request.getHeader("X-User-Id");
        
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        
        // 备选：从请求参数中获取
        userId = request.getParameter("currentUserId");
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        
        return null; // 如果都没有，跳过检查
    }
}
