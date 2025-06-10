package org.xhy.infrastructure.highavailability.dto.request;

import java.util.Map;

/**
 * API实例更新请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceUpdateRequest {

    private String userId;

    private String apiIdentifier;

    private Map<String, Object> routingParams;

    private Map<String, Object> metadata;

    public ApiInstanceUpdateRequest() {
    }

    public ApiInstanceUpdateRequest(String userId, String apiIdentifier, Map<String, Object> routingParams, Map<String, Object> metadata) {
        this.userId = userId;
        this.apiIdentifier = apiIdentifier;
        this.routingParams = routingParams;
        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public Map<String, Object> getRoutingParams() {
        return routingParams;
    }

    public void setRoutingParams(Map<String, Object> routingParams) {
        this.routingParams = routingParams;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "ApiInstanceUpdateRequest{" +
                "userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", routingParams=" + routingParams +
                ", metadata=" + metadata +
                '}';
    }
} 