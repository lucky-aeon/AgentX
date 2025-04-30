package org.xhy.domain.llm.model.config;

import java.io.Serializable;

/** 服务商配置 */
public class ProviderConfig implements Serializable {

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
