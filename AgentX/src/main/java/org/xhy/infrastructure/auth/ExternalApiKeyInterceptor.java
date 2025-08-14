package org.xhy.infrastructure.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xhy.domain.apikey.service.ApiKeyDomainService;
import org.xhy.domain.apikey.model.ApiKeyEntity;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.api.common.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 外部API Key拦截器 用于验证外部API请求的API Key */
@Component
public class ExternalApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiKeyInterceptor.class);

    private final ApiKeyDomainService apiKeyDomainService;
    private final ObjectMapper objectMapper;

    // API Key 请求头名称
    private static final String API_KEY_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public ExternalApiKeyInterceptor(ApiKeyDomainService apiKeyDomainService, ObjectMapper objectMapper) {
        this.apiKeyDomainService = apiKeyDomainService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("拦截外部API请求: {} {}", method, requestURI);


        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 清理上下文，避免内存泄漏
        ExternalApiContext.clear();
        logger.debug("外部API上下文已清理");
    }

    /** 从请求中提取API Key */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(API_KEY_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /** 写入错误响应 */
    private void writeErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> errorResult = Result.error(statusCode, message);
        String jsonResponse = objectMapper.writeValueAsString(errorResult);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}