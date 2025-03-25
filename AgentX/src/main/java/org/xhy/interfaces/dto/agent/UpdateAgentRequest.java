package org.xhy.interfaces.dto.agent;

import org.xhy.domain.agent.model.AgentTool;
import org.xhy.domain.agent.model.ModelConfig;
import org.xhy.infrastructure.util.ValidationUtils;

import java.util.List;

/**
 * 更新Agent信息的请求对象
 * 整合了基本信息和配置信息
 */
public class UpdateAgentRequest {
    
    // 基本信息字段
    private String name;
    private String avatar;
    private String description;
    private Boolean enabled;

    // 配置信息字段
    private String systemPrompt;
    private String welcomeMessage;
    private ModelConfig modelConfig;
    private List<AgentTool> tools;
    private List<String> knowledgeBaseIds;
    
    // 无参构造方法
    public UpdateAgentRequest() {
    }
    
    /**
     * 全参构造方法
     */
    public UpdateAgentRequest(String name, String avatar, String description, 
                              String systemPrompt, String welcomeMessage, 
                              ModelConfig modelConfig, List<AgentTool> tools, 
                              List<String> knowledgeBaseIds) {
        this.name = name;
        this.avatar = avatar;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.welcomeMessage = welcomeMessage;
        this.modelConfig = modelConfig;
        this.tools = tools;
        this.knowledgeBaseIds = knowledgeBaseIds;
    }
    
    /**
     * 校验请求参数
     */
    public void validate() {
        // 必填字段校验
        ValidationUtils.notEmpty(name, "name");
        ValidationUtils.length(name, 1, 50, "name");
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public String getWelcomeMessage() {
        return welcomeMessage;
    }
    
    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }
    
    public ModelConfig getModelConfig() {
        return modelConfig;
    }
    
    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }
    
    public List<AgentTool> getTools() {
        return tools;
    }
    
    public void setTools(List<AgentTool> tools) {
        this.tools = tools;
    }
    
    public List<String> getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }
    
    public void setKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }
} 