package org.xhy.infrastructure.highavailability.dto.request;

/**
 * API实例创建请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceCreateRequest {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * API标识符，必填
     */
    private String apiIdentifier;

    /**
     * API类型，必填
     */
    private String apiType;

    /**
     * 业务ID，必填
     */
    private String businessId;

    public ApiInstanceCreateRequest() {
    }

    public ApiInstanceCreateRequest(String userId, String apiIdentifier, String apiType, String businessId) {
        this.userId = userId;
        this.apiIdentifier = apiIdentifier;
        this.apiType = apiType;
        this.businessId = businessId;
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

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }
} 