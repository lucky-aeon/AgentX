package org.xhy.infrastructure.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author shilong.zang
 * @date 13:58 <br/>
 */
@Configuration
@ConfigurationProperties(prefix = "rerank")
public class RerankProperties {

    /**
     * 嵌入服务名称
     */
    private String name;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API URL
     */
    private String apiUrl;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 请求超时时间(毫秒)
     */
    private int timeout;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
