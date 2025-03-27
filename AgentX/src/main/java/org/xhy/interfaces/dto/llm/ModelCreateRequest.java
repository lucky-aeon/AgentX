package org.xhy.interfaces.dto.llm;

import org.xhy.domain.llm.model.config.ModelConfig;
import org.xhy.domain.llm.model.enums.ModelType;

/**
 * 模型创建请求
 */
public class ModelCreateRequest {
    
    private String providerId;
    private String code;
    private String name;
    private String description;
    private ModelType type;
    private ModelConfig config;
    private Boolean isOfficial = false;
    private Boolean status = true;
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
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
} 