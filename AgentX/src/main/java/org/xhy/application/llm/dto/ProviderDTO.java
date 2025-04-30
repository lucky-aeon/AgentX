package org.xhy.application.llm.dto;

import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 服务提供商DTO */
public class ProviderDTO {

    /** 服务商id */
    private String id;
    /** 服务商协议 */
    private ProviderProtocol protocol;
    /** 服务商名称 */
    private String name;
    /** 服务商描述 */
    private String description;
    /** 服务商配置 */
    private ProviderConfig config;
    /** 是否官方 */
    private Boolean isOfficial;
    /** 服务商状态 */
    private Boolean status;
    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
    /** 模型列表 */
    private List<ModelDTO> models = new ArrayList<>();

    /** 脱敏配置信息（用于返回前端） */
    public void maskSensitiveInfo() {
        if (this.config != null) {
            // 如果有API Key，则脱敏处理
            if (this.config.getApiKey() != null && !this.config.getApiKey().isEmpty()) {
                this.config.setApiKey("***********");
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProviderProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ProviderProtocol protocol) {
        this.protocol = protocol;
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

    public ProviderConfig getConfig() {
        return config;
    }

    public void setConfig(ProviderConfig config) {
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

    public List<ModelDTO> getModels() {
        return models;
    }

    public void setModels(List<ModelDTO> models) {
        this.models = models;
    }
}