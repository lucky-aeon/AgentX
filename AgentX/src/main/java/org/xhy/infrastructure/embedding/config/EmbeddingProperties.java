package org.xhy.infrastructure.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI嵌入服务配置属性类
 * 用于绑定application.yml中的embedding配置
 * @author zang
 */
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

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

    /**
     * 获取嵌入服务名称
     * @return 嵌入服务名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置嵌入服务名称
     * @param name 嵌入服务名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取API密钥
     * @return API密钥
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置API密钥
     * @param apiKey API密钥
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 获取API URL
     * @return API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * 设置API URL
     * @param apiUrl API URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * 获取模型名称
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取超时时间
     * @return 超时时间(毫秒)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 设置超时时间
     * @param timeout 超时时间(毫秒)
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
