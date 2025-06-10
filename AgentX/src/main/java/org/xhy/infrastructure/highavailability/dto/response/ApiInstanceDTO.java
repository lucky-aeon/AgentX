package org.xhy.infrastructure.highavailability.dto.response;

import java.util.Map;

/**
 * API实例DTO - 与网关端保持一致
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceDTO {

    /**
     * 实例ID
     */
    private String id;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * API标识符
     */
    private String apiIdentifier;

    /**
     * API类型
     */
    private String apiType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 路由参数
     */
    private Map<String, Object> routingParams;

    /**
     * 实例状态
     */
    private String status;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    public ApiInstanceDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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

    public Map<String, Object> getRoutingParams() {
        return routingParams;
    }

    public void setRoutingParams(Map<String, Object> routingParams) {
        this.routingParams = routingParams;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "ApiInstanceDTO{" +
                "id='" + id + '\'' +
                ", projectId='" + projectId + '\'' +
                ", projectName='" + projectName + '\'' +
                ", userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType='" + apiType + '\'' +
                ", businessId='" + businessId + '\'' +
                ", routingParams=" + routingParams +
                ", status='" + status + '\'' +
                ", metadata=" + metadata +
                '}';
    }
} 