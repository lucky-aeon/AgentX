package org.xhy.infrastructure.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.infrastructure.utils.JwtUtils;

/** 用户鉴权拦截器 用于拦截需要鉴权的请求，验证用户身份并设置用户上下文 */
@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthInterceptor.class);

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 从请求头中获取token
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 提取token
            String token = authHeader.substring(BEARER_PREFIX.length());

            // 验证token
            if (!JwtUtils.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 从token中获取用户ID并设置到上下文
            String userId = JwtUtils.getUserIdFromToken(token);
            UserContext.setCurrentUserId(userId);

            return true;
        } catch (Exception e) {
            logger.error("用户鉴权失败", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        // 请求结束后清除上下文，防止内存泄漏
        UserContext.clear();
    }
}