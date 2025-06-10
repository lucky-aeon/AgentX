package org.xhy.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 高可用网关配置属性类
 * 用于集中管理所有与高可用网关相关的配置参数
 * 
 * @author xhy
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "high-availability")
public class HighAvailabilityProperties {

    /**
     * 是否启用高可用功能
     */
    private boolean enabled = false;

    /**
     * 高可用网关基础URL
     */
    private String gatewayUrl;

    /**
     * API密钥
     */
    private String apiKey;



    /**
     * 连接超时时间(毫秒)，默认30秒
     */
    private int connectTimeout = 30000;

    /**
     * 读取超时时间(毫秒)，默认60秒
     */
    private int readTimeout = 60000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGatewayUrl() {
        return gatewayUrl+"/api";
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
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