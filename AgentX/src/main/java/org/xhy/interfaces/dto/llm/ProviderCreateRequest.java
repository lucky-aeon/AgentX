package org.xhy.interfaces.dto.llm;

import org.xhy.domain.llm.model.config.ProviderConfig;

/**
 * 服务提供商创建请求
 */
public class ProviderCreateRequest {
    
    private String code;
    private String name;
    private String description;
    private ProviderConfig config;
    private Boolean isOfficial = false;
    private Boolean status = true;
    
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
} 