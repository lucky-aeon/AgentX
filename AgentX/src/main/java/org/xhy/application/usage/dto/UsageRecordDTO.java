package org.xhy.application.usage.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** 使用记录DTO 用户使用记录信息传输对象 */
public class UsageRecordDTO {

    /** 记录ID */
    private String id;

    /** 用户ID */
    private String userId;

    /** 关联的商品ID */
    private String productId;

    /** 用量数据 */
    private Map<String, Object> quantityData;

    /** 本次用量产生的费用 */
    private BigDecimal cost;

    /** 原始请求ID */
    private String requestId;

    /** 计费发生时间 */
    private LocalDateTime billedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 业务服务名称 （如：GPT-4 模型调用） */
    private String serviceName;

    /** 服务类型 （如：模型服务） */
    private String serviceType;

    /** 服务描述 */
    private String serviceDescription;

    /** 定价规则说明 （如：输入 ¥0.002/1K tokens，输出 ¥0.006/1K tokens） */
    private String pricingRule;

    /** 关联实体名称 （如：具体的模型名称或Agent名称） */
    private String relatedEntityName;

    public UsageRecordDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Map<String, Object> getQuantityData() {
        return quantityData;
    }

    public void setQuantityData(Map<String, Object> quantityData) {
        this.quantityData = quantityData;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getBilledAt() {
        return billedAt;
    }

    public void setBilledAt(LocalDateTime billedAt) {
        this.billedAt = billedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getPricingRule() {
        return pricingRule;
    }

    public void setPricingRule(String pricingRule) {
        this.pricingRule = pricingRule;
    }

    public String getRelatedEntityName() {
        return relatedEntityName;
    }

    public void setRelatedEntityName(String relatedEntityName) {
        this.relatedEntityName = relatedEntityName;
    }
}