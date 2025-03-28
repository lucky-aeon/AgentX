package org.xhy.interfaces.dto.agent;

import org.xhy.domain.agent.model.AgentTool;
import org.xhy.domain.agent.constant.AgentType;
import org.xhy.domain.agent.model.ModelConfig;
import org.xhy.infrastructure.util.ValidationUtils;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 创建Agent的请求对象
 */
public class CreateAgentRequest {

    @NotBlank(message = "助理名称不可为空")
    private String name;
    private String description;
    private String avatar;
    private AgentType agentType = AgentType.CHAT_ASSISTANT;
    private String systemPrompt;
    private String welcomeMessage;
    private ModelConfig modelConfig;
    private List<AgentTool> tools;
    private List<String> knowledgeBaseIds;

    // 构造方法
    public CreateAgentRequest() {
    }
    // Getter和Setter
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    // 为了兼容旧代码，提供Integer getter/setter
    public Integer getAgentTypeCode() {
        return agentType != null ? agentType.getCode() : null;
    }

    public void setAgentTypeCode(Integer agentTypeCode) {
        this.agentType = AgentType.fromCode(agentTypeCode);
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