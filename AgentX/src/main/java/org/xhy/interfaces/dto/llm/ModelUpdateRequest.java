package org.xhy.interfaces.dto.llm;

import org.xhy.domain.llm.model.config.ModelConfig;

/**
 * 模型更新请求
 */
public class ModelUpdateRequest {
    
    private String id;
    private String name;
    private String description;
    private ModelConfig config;
    private Boolean status;
    
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ModelConfig getConfig() {
        return config;
    }
    
    public void setConfig(ModelConfig config) {
        this.config = config;
    }
    
    public Boolean getStatus() {
        return status;
    }
    
    public void setStatus(Boolean status) {
        this.status = status;
    }
} 