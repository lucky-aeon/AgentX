package org.xhy.interfaces.dto.agent;

import org.xhy.domain.agent.model.AgentTool;
import org.xhy.domain.agent.model.ModelConfig;
import org.xhy.infrastructure.util.ValidationUtils;

import java.util.List;

/**
 * 更新Agent配置的请求对象
 */
public class UpdateAgentConfigRequest {
    
    private String systemPrompt;
    private String welcomeMessage;
    private ModelConfig modelConfig;
    private List<AgentTool> tools;
    private List<String> knowledgeBaseIds;
    
    // 构造方法
    public UpdateAgentConfigRequest() {
    }
    
    public UpdateAgentConfigRequest(String systemPrompt, String welcomeMessage, ModelConfig modelConfig, 
                                 List<AgentTool> tools, List<String> knowledgeBaseIds) {
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
        ValidationUtils.notEmpty(systemPrompt, "systemPrompt");
        // 其他字段可以为空，不做强制校验
    }
    
    // Getter和Setter
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