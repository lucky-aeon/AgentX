package org.xhy.infrastructure.llm.config;

import java.util.HashMap;
import java.util.Map;

public class ProviderConfig {


    /**
     * 密钥
     */
    private final String apiKey;

    /**
     * baseUrl
     */
    private final String baseUrl;

    private String model;


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    private Map<String,String> customHeaders = new HashMap<>();

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public ProviderConfig(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public void addCustomHeaders(String key,String value) {
        customHeaders.put(key,value);
    }
}
