package org.xhy.infrastructure.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 用户鉴权拦截器 用于拦截需要鉴权的请求，验证用户身份并设置用户上下文 */
@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 验证用户身份（正常情况下应该从Token中解析用户ID）
            // 但根据需求，这里直接mock一个用户ID为1
            String userId = "1";
            logger.debug("设置用户ID: {}", userId);

            // 将用户ID设置到上下文
            UserContext.setCurrentUserId(userId);
            return true;
        } catch (Exception e) {
            logger.error("用户鉴权失败", e);
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
