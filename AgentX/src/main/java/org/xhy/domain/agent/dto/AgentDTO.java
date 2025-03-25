package org.xhy.domain.agent.dto;

import org.xhy.domain.agent.constant.AgentStatus;
import org.xhy.domain.agent.constant.AgentType;
import org.xhy.domain.agent.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent数据传输对象，用于表示层和应用层之间传递数据
 */
public class AgentDTO {

    /**
     * Agent唯一ID
     */
    private String id;

    /**
     * Agent名称
     */
    private String name;

    /**
     * Agent头像URL
     */
    private String avatar;

    /**
     * Agent描述
     */
    private String description;

    /**
     * Agent系统提示词
     */
    private String systemPrompt;

    /**
     * 欢迎消息
     */
    private String welcomeMessage;

    /**
     * 模型配置，包含模型类型、温度等参数
     */
    private ModelConfig modelConfig;

    /**
     * Agent可使用的工具列表
     */
    private List<AgentTool> tools;

    /**
     * 关联的知识库ID列表
     */
    private List<String> knowledgeBaseIds;

    /**
     * 当前发布的版本ID
     */
    private String publishedVersion;

    /**
     * Agent状态：true-启用，false-禁用
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * Agent类型：1-聊天助手, 2-功能性Agent
     */
    private Integer agentType;

    /**
     * 创建者用户ID
     */
    private String userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 无参构造函数
     */
    public AgentDTO() {
        this.modelConfig = ModelConfig.createDefault();
        this.tools = new ArrayList<>();
        this.knowledgeBaseIds = new ArrayList<>();
    }

    // Getter和Setter方法
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

    public String getPublishedVersion() {
        return publishedVersion;
    }

    public void setPublishedVersion(String publishedVersion) {
        this.publishedVersion = publishedVersion;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getAgentType() {
        return agentType;
    }

    public void setAgentType(Integer agentType) {
        this.agentType = agentType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    /**
     * 获取状态文本描述
     */
    public String getStatusText() {
        return AgentStatus.fromCode(enabled ? 1 : 0).getDescription();
    }

    /**
     * 获取类型文本描述
     */
    public String getAgentTypeText() {
        return AgentType.fromCode(agentType).getDescription();
    }

    /**
     * 将当前DTO转换为Entity对象
     * @return 转换后的AgentEntity对象
     */
    public AgentEntity toEntity() {
        AgentEntity entity = new AgentEntity();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setAvatar(this.avatar);
        entity.setDescription(this.description);
        entity.setSystemPrompt(this.systemPrompt);
        entity.setWelcomeMessage(this.welcomeMessage);
        entity.setModelConfig(this.modelConfig);
        entity.setTools(this.tools);
        entity.setKnowledgeBaseIds(this.knowledgeBaseIds);
        entity.setPublishedVersion(this.publishedVersion);
        entity.setEnabled(this.enabled);
        entity.setAgentType(this.agentType);
        entity.setUserId(this.userId);
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);
        return entity;
    }
} 