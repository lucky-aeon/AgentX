package org.xhy.domain.llm.model;

import com.baomidou.mybatisplus.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.infrastructure.converter.ProviderConfigConverter;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.EncryptUtils;

import java.time.LocalDateTime;

/**
 * 服务提供商领域模型
 */
@TableName("providers")
public class ProviderEntity {
    private static final Logger log = LoggerFactory.getLogger(ProviderEntity.class);

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
            log.debug("设置配置: apiKey={}, baseUrl={}", 
                config.getApiKey() != null ? "非空" : "null", 
                config.getBaseUrl() != null ? "非空" : "null");
            encryptConfigFields();
        } else {
            log.debug("设置配置: null");
        }
    }

    /**
     * 获取原始配置（不解密）
     *
     * @return 原始配置（可能已加密）
     */
    public ProviderConfig getEncryptedConfig() {
        if (this.config != null) {
            log.debug("获取加密配置: apiKey={}, baseUrl={}", 
                this.config.getApiKey() != null ? "非空" : "null", 
                this.config.getBaseUrl() != null ? "非空" : "null");
        } else {
            log.debug("获取加密配置: null");
        }
        return this.config;
    }

    /**
     * 获取配置（自动解密敏感信息）
     *
     * @return 解密后的配置
     */
    public ProviderConfig getConfig() {
        ProviderConfig decryptedConfig = getDecryptedConfig();
        if (decryptedConfig != null) {
            log.debug("获取解密配置: apiKey={}, baseUrl={}", 
                decryptedConfig.getApiKey() != null ? "非空" : "null", 
                decryptedConfig.getBaseUrl() != null ? "非空" : "null");
        } else {
            log.debug("获取解密配置: null");
        }
        return decryptedConfig;
    }

    /**
     * 加密配置中的敏感字段
     */
    private void encryptConfigFields() {
        if (config != null) {
            if (config.getApiKey() != null && !isEncrypted(config.getApiKey())) {
                log.debug("加密apiKey: 原始长度={}", config.getApiKey().length());
                config.setApiKey(EncryptUtils.encrypt(config.getApiKey()));
                log.debug("加密后长度={}", config.getApiKey().length());
            }
        }
    }

    /**
     * 简单判断字符串是否已加密
     */
    private boolean isEncrypted(String str) {
        // 简单判断：如果长度很长且包含特定字符组合，可能是已加密的
        return str != null && str.length() > 20 && str.contains("==");
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
                try {
                    String apiKey = this.config.getApiKey();
                    // 如果看起来像已加密的内容，则解密
                    if (isEncrypted(apiKey)) {
                        log.debug("解密apiKey: 加密长度={}", apiKey.length());
                        decryptedConfig.setApiKey(EncryptUtils.decrypt(apiKey));
                        log.debug("解密后长度={}", decryptedConfig.getApiKey().length());
                    } else {
                        // 否则直接使用原始值
                        log.debug("apiKey未加密，直接使用原值");
                        decryptedConfig.setApiKey(apiKey);
                    }
                } catch (Exception e) {
                    log.error("解密失败，使用原始值", e);
                    decryptedConfig.setApiKey(this.config.getApiKey());
                }
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

    public void isActive() {
        if (!status){
            throw new BusinessException("服务商未激活");
        }
    }
}