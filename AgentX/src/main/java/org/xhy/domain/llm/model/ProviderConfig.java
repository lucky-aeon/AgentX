package org.xhy.domain.llm.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.util.ConfigEncryptor;

import java.time.LocalDateTime;

@TableName("llm_provider_configs")
public class ProviderConfig {
    private Long id;
    private String name;
    private ProviderType providerType;
    private String encryptedConfig;
    
    @TableField(exist = false)
    private ProviderCredentials credentials;
    
    private boolean isOfficial;
    private Long userId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    public String getEncryptedConfig() {
        return encryptedConfig;
    }

    public void setEncryptedConfig(String encryptedConfig) {
        this.encryptedConfig = encryptedConfig;
    }

    public ProviderCredentials getCredentials() {
        if (credentials == null && encryptedConfig != null) {
            credentials = ConfigEncryptor.decrypt(encryptedConfig, ProviderCredentials.class);
        }
        return credentials;
    }

    public void setCredentials(ProviderCredentials credentials) {
        this.credentials = credentials;
        this.encryptedConfig = ConfigEncryptor.encrypt(credentials);
    }

    public boolean isOfficial() {
        return isOfficial;
    }

    public void setOfficial(boolean official) {
        isOfficial = official;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
} 