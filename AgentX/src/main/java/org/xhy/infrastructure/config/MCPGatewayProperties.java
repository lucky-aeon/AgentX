package org.xhy.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Gateway网关配置属性类
 * 用于集中管理所有与MCP网关相关的配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.gateway")
public class MCPGatewayProperties {

    private String baseUrl; // 网关基础URL
    private String apiKey; // API密钥
    private int connectTimeout = 30000; // 连接超时(毫秒)，默认30秒
    private int readTimeout = 60000; // 读取超时(毫秒)，默认60秒

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}