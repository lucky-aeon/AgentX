package org.xhy.infrastructure.llm.config;

import java.util.HashMap;
import java.util.Map;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

public class ProviderConfig {

    /** 密钥 */
    private final String apiKey;

    /** baseUrl */
    private final String baseUrl;

    /** 模型 */
    private String model;

    private ProviderProtocol protocol;

    public ProviderProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ProviderProtocol protocol) {
        this.protocol = protocol;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    private Map<String, String> customHeaders = new HashMap<>();

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public ProviderConfig(String apiKey, String baseUrl, String model, ProviderProtocol protocol) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.protocol = protocol;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public void addCustomHeaders(String key, String value) {
        customHeaders.put(key, value);
    }
}
