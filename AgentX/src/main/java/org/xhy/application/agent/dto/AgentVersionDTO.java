package org.xhy.application.agent.dto;

import org.xhy.domain.agent.constant.AgentType;
import org.xhy.domain.agent.model.AgentModelConfig;
import org.xhy.domain.agent.model.AgentTool;
import org.xhy.domain.agent.constant.PublishStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent版本数据传输对象，用于表示层和应用层之间传递Agent版本数据
 */
public class AgentVersionDTO {

    /**
     * 版本唯一ID
     */
    private String id;

    /**
     * 关联的Agent ID
     */
    private String agentId;

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
     * 版本号，如1.0.0
     */
    private String versionNumber;

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
    private AgentModelConfig modelConfig;

    /**
     * Agent可使用的工具列表
     */
    private List<AgentTool> tools;

    /**
     * 关联的知识库ID列表
     */
    private List<String> knowledgeBaseIds;

    /**
     * 版本更新日志
     */
    private String changeLog;

    /**
     * Agent类型：1-聊天助手, 2-功能性Agent
     */
    private Integer agentType;

    /**
     * 发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架
     */
    private Integer publishStatus;

    /**
     * 审核拒绝原因
     */
    private String rejectReason;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

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
    public AgentVersionDTO() {
        this.modelConfig = AgentModelConfig.createDefault();
        this.tools = new ArrayList<>();
        this.knowledgeBaseIds = new ArrayList<>();
    }

    /**
     * 全参构造函数
     */
    public AgentVersionDTO(String id, String agentId, String name, String avatar, String description,
            String versionNumber, String systemPrompt, String welcomeMessage,
                           AgentModelConfig modelConfig, List<AgentTool> tools, List<String> knowledgeBaseIds,
            String changeLog, Integer agentType, Integer publishStatus,
            String rejectReason, LocalDateTime reviewTime, LocalDateTime publishedAt,
            String userId, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.agentId = agentId;
        this.name = name;
        this.avatar = avatar;
        this.description = description;
        this.versionNumber = versionNumber;
        this.systemPrompt = systemPrompt;
        this.welcomeMessage = welcomeMessage;
        this.modelConfig = modelConfig;
        this.tools = tools;
        this.knowledgeBaseIds = knowledgeBaseIds;
        this.changeLog = changeLog;
        this.agentType = agentType;
        this.publishStatus = publishStatus;
        this.rejectReason = rejectReason;
        this.reviewTime = reviewTime;
        this.publishedAt = publishedAt;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
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

    public AgentModelConfig getModelConfig() {
        return modelConfig;
    }

    public void setModelConfig(AgentModelConfig modelConfig) {
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

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public Integer getAgentType() {
        return agentType;
    }

    public void setAgentType(Integer agentType) {
        this.agentType = agentType;
    }

    /**
     * 获取类型文本描述
     */
    public String getAgentTypeText() {
        return AgentType.fromCode(agentType).getDescription();
    }

    public Integer getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(Integer publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
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
     * 获取发布状态的描述文本
     */
    public String getPublishStatusText() {
        return PublishStatus.fromCode(publishStatus).getDescription();
    }

    /**
     * 是否已发布状态
     */
    public boolean isPublished() {
        return PublishStatus.PUBLISHED.getCode().equals(publishStatus);
    }

    /**
     * 是否被拒绝状态
     */
    public boolean isRejected() {
        return PublishStatus.REJECTED.getCode().equals(publishStatus);
    }

    /**
     * 是否审核中状态
     */
    public boolean isReviewing() {
        return PublishStatus.REVIEWING.getCode().equals(publishStatus);
    }

    /**
     * 是否已下架状态
     */
    public boolean isRemoved() {
        return PublishStatus.REMOVED.getCode().equals(publishStatus);
    }
}