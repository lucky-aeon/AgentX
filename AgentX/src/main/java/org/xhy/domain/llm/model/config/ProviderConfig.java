package org.xhy.domain.llm.model.config;

/**
 * 服务商配置
 */
public class ProviderConfig {
    
    private String apiKey;
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}