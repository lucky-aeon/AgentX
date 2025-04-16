package org.xhy.interfaces.dto.agent.request;

import jakarta.validation.constraints.NotBlank;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.agent.model.AgentTool;

import java.util.List;

/**
 * 更新Agent信息的请求对象
 * 整合了基本信息和配置信息
 */
public class UpdateAgentRequest {


    private String agentId;
    @NotBlank(message = "助理名称不可为空")
    private String name;
    private String avatar;
    private String description;
    private Boolean enabled;

    // 配置信息字段
    private String systemPrompt;
    private String welcomeMessage;
    private LLMModelConfig modelConfig;
    private List<AgentTool> tools;
    private List<String> knowledgeBaseIds;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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