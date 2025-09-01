package org.xhy.infrastructure.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 知识图谱API密钥认证过滤器 使用纯Servlet Filter实现，无需Spring Security
 * 
 * @author zang */
@Component
@ConfigurationProperties(prefix = "agentx.graph.security")
public class GraphApiKeyAuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(GraphApiKeyAuthenticationFilter.class);

    private boolean enabled = true;
    private String apiKey = "GRAPH-API-KEY-2024-CHANGE-ME-IN-PRODUCTION";
    private String headerName = "X-Graph-API-Key";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("初始化知识图谱API密钥认证过滤器，认证: {}", enabled ? "启用" : "禁用");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 只对知识图谱API进行认证
        if (!requestPath.startsWith("/api/v1/graph")) {
            chain.doFilter(request, response);
            return;
        }

        // 健康检查端点无需认证
        if (requestPath.equals("/api/v1/graph/health")) {
            chain.doFilter(request, response);
            return;
        }

        // 如果认证被禁用，直接通过
        if (!enabled) {
            logger.debug("知识图谱API认证已禁用，允许请求: {} {}", method, requestPath);
            chain.doFilter(request, response);
            return;
        }

        logger.debug("处理知识图谱API认证请求: {} {}", method, requestPath);

        // 验证API密钥
        String requestApiKey = httpRequest.getHeader(headerName);

        if (requestApiKey == null || requestApiKey.trim().isEmpty()) {
            logger.warn("API密钥缺失，请求路径: {} {}", method, requestPath);
            sendUnauthorizedResponse(httpResponse, "API密钥缺失，请在请求头中添加 " + headerName);
            return;
        }

        if (!isValidApiKey(requestApiKey)) {
            logger.warn("API密钥无效，请求路径: {} {}，提供的密钥: {}", method, requestPath, maskApiKey(requestApiKey));
            sendUnauthorizedResponse(httpResponse, "API密钥无效");
            return;
        }

        logger.debug("知识图谱API认证成功: {} {}", method, requestPath);

        // 认证成功，继续处理请求
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("销毁知识图谱API密钥认证过滤器");
    }

    /** 验证API密钥 */
    private boolean isValidApiKey(String requestApiKey) {
        return apiKey != null && apiKey.equals(requestApiKey.trim());
    }

    /** 发送未授权响应 */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String jsonResponse = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}", message,
                System.currentTimeMillis());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /** 掩码API密钥用于日志记录 */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return apiKey.substring(0, 2) + "****" + apiKey.substring(apiKey.length() - 2);
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}