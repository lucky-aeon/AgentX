package org.xhy.application.agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/** Agent数据传输对象（包含用户信息），用于管理员页面显示 */
public class AgentWithUserDTO {

    /** Agent唯一ID */
    private String id;

    /** Agent名称 */
    private String name;

    /** Agent头像URL */
    private String avatar;

    /** Agent描述 */
    private String description;

    /** Agent系统提示词 */
    private String systemPrompt;

    /** 欢迎消息 */
    private String welcomeMessage;

    /** Agent可使用的工具列表 */
    private List<String> toolIds;

    /** 关联的知识库ID列表 */
    private List<String> knowledgeBaseIds;

    /** 当前发布的版本ID */
    private String publishedVersion;

    /** Agent状态：true-启用，false-禁用 */
    private Boolean enabled = Boolean.TRUE;

    /** 创建者用户ID */
    private String userId;

    /** 创建者用户昵称 */
    private String userNickname;

    /** 创建者邮箱 */
    private String userEmail;

    /** 创建者头像 */
    private String userAvatarUrl;

    private Map<String, Map<String, Map<String, String>>> toolPresetParams;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 是否支持多模态 */
    private Boolean multiModal;

    /** Agent的版本列表 */
    private List<AgentVersionDTO> versions = new ArrayList<>();

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

    public List<String> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<String> toolIds) {
        this.toolIds = toolIds;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
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

    public Map<String, Map<String, Map<String, String>>> getToolPresetParams() {
        return toolPresetParams;
    }

    public void setToolPresetParams(Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        this.toolPresetParams = toolPresetParams;
    }

    public Boolean getMultiModal() {
        return multiModal;
    }

    public void setMultiModal(Boolean multiModal) {
        this.multiModal = multiModal;
    }

    public List<AgentVersionDTO> getVersions() {
        return versions;
    }

    public void setVersions(List<AgentVersionDTO> versions) {
        this.versions = versions;
    }
}