package org.xhy.application.llm.dto;

import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.llm.model.enums.ModelType;

import java.time.LocalDateTime;

/** 模型数据传输对象 */
public class ModelDTO {

    /** 模型id */
    private String id;
    /** 用户id */
    private String userId;
    /** 服务商id */
    private String providerId;
    /** 服务商名称 */
    private String providerName; // 额外添加，便于前端显示
    /** 模型id */
    private String modelId;
    /** 模型名称 */
    private String name;
    /** 模型描述 */
    private String description;
    /** 模型类型 */
    private ModelType type;
    /** 模型部署名称 */
    private String modelEndpoint;
    /** 是否官方 */
    private Boolean isOfficial;
    /** 模型状态 */
    private Boolean status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public String getModelEndpoint() {
        return modelEndpoint;
    }

    public void setModelEndpoint(String modelEndpoint) {
        this.modelEndpoint = modelEndpoint;
    }

    public Boolean getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Boolean isOfficial) {
        this.isOfficial = isOfficial;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
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
}