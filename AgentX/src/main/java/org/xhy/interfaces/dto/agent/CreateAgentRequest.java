package org.xhy.interfaces.dto.agent;

import org.xhy.domain.agent.model.AgentTool;
import org.xhy.domain.agent.constant.AgentType;
import org.xhy.domain.agent.model.ModelConfig;
import org.xhy.infrastructure.util.ValidationUtils;

import java.util.List;

/**
 * 创建Agent的请求对象
 */
public class CreateAgentRequest {

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

    public CreateAgentRequest(String name, String description, String avatar, AgentType agentType,
            String systemPrompt, String welcomeMessage, ModelConfig modelConfig,
            List<AgentTool> tools, List<String> knowledgeBaseIds, String userId) {
        this.name = name;
        this.description = description;
        this.avatar = avatar;
        this.agentType = agentType;
        this.systemPrompt = systemPrompt;
        this.welcomeMessage = welcomeMessage;
        this.modelConfig = modelConfig;
        this.tools = tools;
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    // 兼容旧构造方法
    public CreateAgentRequest(String name, String description, String avatar, Integer agentTypeCode,
            String systemPrompt, String welcomeMessage, ModelConfig modelConfig,
            List<AgentTool> tools, List<String> knowledgeBaseIds, String userId) {
        this(name, description, avatar,
                agentTypeCode != null ? AgentType.fromCode(agentTypeCode) : AgentType.CHAT_ASSISTANT,
                systemPrompt, welcomeMessage, modelConfig, tools, knowledgeBaseIds, userId);
    }

    /**
     * 校验请求参数
     */
    public void validate() {
        ValidationUtils.notEmpty(name, "name");
        ValidationUtils.length(name, 1, 50, "name");

        // Agent类型校验，如果不提供，默认为聊天助手
        if (agentType == null) {
            agentType = AgentType.CHAT_ASSISTANT;
        }
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