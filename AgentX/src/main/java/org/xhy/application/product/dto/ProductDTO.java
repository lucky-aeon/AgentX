package org.xhy.application.product.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** 商品DTO */
public class ProductDTO {

    /** 商品唯一ID */
    private String id;

    /** 商品名称 */
    private String name;

    /** 商品类型 */
    private String type;

    /** 关联的业务ID */
    private String serviceId;

    /** 关联的规则ID */
    private String ruleId;

    /** 价格配置 */
    private Map<String, Object> pricingConfig;

    /** 状态 1-激活 0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 模型名称 (仅MODEL_USAGE类型) */
    private String modelName;

    /** 模型标识符 (仅MODEL_USAGE类型) */
    private String modelId;

    /** 服务商名称 (仅MODEL_USAGE类型) */
    private String providerName;

    public ProductDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Map<String, Object> getPricingConfig() {
        return pricingConfig;
    }

    public void setPricingConfig(Map<String, Object> pricingConfig) {
        this.pricingConfig = pricingConfig;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}