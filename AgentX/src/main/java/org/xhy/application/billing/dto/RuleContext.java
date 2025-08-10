package org.xhy.application.billing.dto;

import java.util.Map;

/** 规则上下文 封装计费所需的所有信息 */
public class RuleContext {

    /** 规则类型 (如：MODEL_USAGE, AGENT_CREATION) */
    private String type;

    /** 服务ID (业务标识，如模型ID、固定标识等) */
    private String serviceId;

    /** 用量数据 (如：{"input": 1000, "output": 500}) */
    private Map<String, Object> usageData;

    /** 请求ID，用于幂等性控制 */
    private String requestId;

    /** 用户ID */
    private String userId;

    public RuleContext() {
    }

    public RuleContext(String type, String serviceId, Map<String, Object> usageData, String requestId, String userId) {
        this.type = type;
        this.serviceId = serviceId;
        this.usageData = usageData;
        this.requestId = requestId;
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Map<String, Object> getUsageData() {
        return usageData;
    }

    public void setUsageData(Map<String, Object> usageData) {
        this.usageData = usageData;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /** 构建器模式 */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String serviceId;
        private Map<String, Object> usageData;
        private String requestId;
        private String userId;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder usageData(Map<String, Object> usageData) {
            this.usageData = usageData;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public RuleContext build() {
            return new RuleContext(type, serviceId, usageData, requestId, userId);
        }
    }

    /** 验证上下文数据是否有效 */
    public boolean isValid() {
        return type != null && !type.trim().isEmpty() && serviceId != null && !serviceId.trim().isEmpty()
                && usageData != null && !usageData.isEmpty() && userId != null && !userId.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "BillingContext{" + "type='" + type + '\'' + ", serviceId='" + serviceId + '\'' + ", usageData="
                + usageData + ", requestId='" + requestId + '\'' + ", userId='" + userId + '\'' + '}';
    }
}