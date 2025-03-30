package org.xhy.domain.llm.model;

import org.xhy.domain.llm.model.config.ProviderConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务提供商聚合根
 * 注意：这是领域模型中的聚合根，包含领域逻辑，与ProviderEntity（持久化实体）区分
 */
public class ProviderAggregate {
    
    private ProviderEntity entity;
    private List<ModelEntity> models;

    public ProviderAggregate(ProviderEntity entity, List<ModelEntity> models) {
        this.entity = entity;
        this.models = models;
    }

    /**
     * 添加模型
     */
    public void addModel(ModelEntity model) {
        if (model != null && model.getProviderId().equals(entity.getId())) {
            models.add(model);
        }
    }
    
    /**
     * 设置模型列表
     */
    public void setModels(List<ModelEntity> models) {
        this.models = models != null ? models : new ArrayList<>();
    }
    
    /**
     * 获取模型列表
     */
    public List<ModelEntity> getModels() {
        return models;
    }
    
    /**
     * 获取服务商配置（解密版本）
     */
    public ProviderConfig getConfig() {
        return entity.getConfig();  // 已解密
    }
    
    /**
     * 设置服务商配置（会自动加密）
     */
    public void setConfig(ProviderConfig config) {
        entity.setConfig(config);  // 会自动加密
    }
    
    // 委托方法，代理到实体
    
    public String getId() {
        return entity.getId();
    }
    
    public String getUserId() {
        return entity.getUserId();
    }
    
    public String getCode() {
        return entity.getProtocol();
    }
    
    public void setCode(String code) {
        entity.setProtocol(code);
    }
    
    public String getName() {
        return entity.getName();
    }
    
    public void setName(String name) {
        entity.setName(name);
    }
    
    public String getDescription() {
        return entity.getDescription();
    }
    
    public void setDescription(String description) {
        entity.setDescription(description);
    }
    
    public Boolean getIsOfficial() {
        return entity.getIsOfficial();
    }
    
    public void setIsOfficial(Boolean isOfficial) {
        entity.setIsOfficial(isOfficial);
    }
    
    public Boolean getStatus() {
        return entity.getStatus();
    }
    
    public void setStatus(Boolean status) {
        entity.setStatus(status);
    }
    
    public LocalDateTime getCreatedAt() {
        return entity.getCreatedAt();
    }
    
    public LocalDateTime getUpdatedAt() {
        return entity.getUpdatedAt();
    }
    
    public LocalDateTime getDeletedAt() {
        return entity.getDeletedAt();
    }
    
    /**
     * 获取原始实体
     */
    public ProviderEntity getEntity() {
        return entity;
    }
} 