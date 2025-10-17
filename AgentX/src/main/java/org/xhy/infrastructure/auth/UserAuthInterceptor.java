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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/** 用户鉴权拦截器 用于拦截需要鉴权的请求，验证用户身份并设置用户上下文 */
@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthInterceptor.class);

    private static final String BEARER_PREFIX = "Bearer ";

    // 不需要检查的路径列表，与WebMvcConfig保持一致
    private static final List<String> EXCLUDED_PATHS = Arrays.asList("/login", "/health", "/register", "/auth/config",
            "/send-email-code", "/verify-email-code", "/get-captcha", "/reset-password", "/send-reset-password-code",
            "/oauth/github/authorize", "/oauth/github/callback");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("JWT认证拦截器处理请求: {} {}", method, requestURI);

        try {
            // 从请求头中获取token
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (!StringUtils.hasText(authHeader)) {
                logger.warn("认证失败 - 缺少Authorization头: {} {}", method, requestURI);
                writeErrorResponse(response, "缺少认证头");
                return false;
            }

            if (!authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("认证失败 - Authorization头格式错误: {} {}, header: {}", method, requestURI, authHeader);
                writeErrorResponse(response, "认证头格式错误");
                return false;
            }

            // 提取token
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (!StringUtils.hasText(token)) {
                logger.warn("认证失败 - Token为空: {} {}", method, requestURI);
                writeErrorResponse(response, "Token为空");
                return false;
            }

            // 验证token
            if (!JwtUtils.validateToken(token)) {
                logger.warn("认证失败 - Token验证失败: {} {}, token前缀: {}", method, requestURI,
                        token.length() > 20 ? token.substring(0, 20) + "..." : token);
                writeErrorResponse(response, "Token无效或已过期");
                return false;
            }

            // 从token中获取用户ID并设置到上下文
            String userId = JwtUtils.getUserIdFromToken(token);

            if (!StringUtils.hasText(userId)) {
                logger.warn("认证失败 - 无法从Token中获取用户ID: {} {}", method, requestURI);
                writeErrorResponse(response, "无效的用户信息");
                return false;
            }

            UserContext.setCurrentUserId(userId);

            logger.debug("认证成功: {} {}, 用户ID: {}", method, requestURI, userId);

            return true;

        } catch (Exception e) {
            logger.error("用户鉴权异常: {} {}, 错误: {}", method, requestURI, e.getMessage(), e);
            writeErrorResponse(response, "认证服务异常");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        // 请求结束后清除上下文，防止内存泄漏
        String userId = UserContext.getCurrentUserId();
        if (userId != null) {
            logger.debug("清除用户上下文: {}", userId);
        }
        UserContext.clear();
    }

    /** 写入错误响应
     * @param response HTTP响应
     * @param message 错误消息 */
    private void writeErrorResponse(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            String jsonResponse = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                    message, System.currentTimeMillis());

            PrintWriter writer = response.getWriter();
            writer.write(jsonResponse);
            writer.flush();

        } catch (Exception e) {
            logger.error("写入认证失败响应时出错", e);
        }
    }
}