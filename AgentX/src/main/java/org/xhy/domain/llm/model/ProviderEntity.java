package org.xhy.domain.llm.model;

import com.baomidou.mybatisplus.annotation.*;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.infrastructure.converter.ProviderConfigConverter;
import org.xhy.infrastructure.utils.EncryptUtils;

import java.time.LocalDateTime;

/**
 * 服务提供商领域模型
 */
@TableName("providers")
public class ProviderEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;
    private String protocol;
    private String name;
    private String description;

    @TableField(typeHandler = ProviderConfigConverter.class)
    private ProviderConfig config;

    private Boolean isOfficial;
    private Boolean status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;

    /**
     * 设置配置并自动加密敏感信息
     *
     * @param config 未加密的配置
     */
    public void setConfig(ProviderConfig config) {
        this.config = config;
        if (config != null) {
            encryptConfigFields();
        }
    }

    /**
     * 获取原始配置（不解密）
     *
     * @return 原始配置（可能已加密）
     */
    public ProviderConfig getEncryptedConfig() {
        return this.config;
    }

    /**
     * 获取配置（自动解密敏感信息）
     *
     * @return 解密后的配置
     */
    public ProviderConfig getConfig() {
        return getDecryptedConfig();
    }

    /**
     * 加密配置中的敏感字段
     */
    private void encryptConfigFields() {
        if (config != null) {
            if (config.getApiKey() != null) {
                config.setApiKey(EncryptUtils.encrypt(config.getApiKey()));
            }
        }
    }

    /**
     * 解密配置中的敏感字段
     *
     * @return 解密后的配置对象的副本
     */
    public ProviderConfig getDecryptedConfig() {
        if (config != null) {
            ProviderConfig decryptedConfig = new ProviderConfig();
            // 复制基本属性
            decryptedConfig.setBaseUrl(this.config.getBaseUrl());

            // 解密敏感信息
            if (this.config.getApiKey() != null) {
                decryptedConfig.setApiKey(EncryptUtils.decrypt(this.config.getApiKey()));
            }
            return decryptedConfig;
        }
        return null;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
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

    public Boolean getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Boolean official) {
        isOfficial = official;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
} 