package org.xhy.application.llm.dto;

import org.xhy.domain.llm.model.config.ModelConfig;
import org.xhy.domain.llm.model.enums.ModelType;

import java.time.LocalDateTime;

/**
 * 模型数据传输对象
 */
public class ModelDTO {
    
    private String id;
    private String userId;
    private String providerId;
    private String providerName; // 额外添加，便于前端显示
    private String code;
    private String name;
    private String description;
    private ModelType type;
    private ModelConfig config;
    private Boolean isOfficial;
    private Boolean status;
    private LocalDateTime createdAt;
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
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
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
    
    public ModelConfig getConfig() {
        return config;
    }
    
    public void setConfig(ModelConfig config) {
        this.config = config;
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