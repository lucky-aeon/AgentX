package org.xhy.interfaces.dto.llm;

import org.xhy.domain.llm.model.config.LLMModelConfig;
import org.xhy.domain.llm.model.enums.ModelType;

/**
 * 模型创建请求
 */
public class ModelCreateRequest {
    
    /**
     * 服务商ID
     */
    private String providerId;

    /**
     * 模型id
     */
    private String modelId;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 模型类型
     */
    private ModelType type;

    /**
     * 模型配置
     */
    private LLMModelConfig config;
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
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
    
    public LLMModelConfig getConfig() {
        return config;
    }
    
    public void setConfig(LLMModelConfig config) {
        this.config = config;
    }

}