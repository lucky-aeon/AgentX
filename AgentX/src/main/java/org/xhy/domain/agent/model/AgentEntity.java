package org.xhy.domain.agent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


import org.xhy.infrastructure.converter.ListStringConverter;
import org.xhy.infrastructure.converter.MapConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Agent实体类，代表一个AI助手 */
@TableName(value = "agents", autoResultMap = true)
public class AgentEntity extends BaseEntity {

    /** Agent唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** Agent名称 */
    @TableField("name")
    private String name;

    /** Agent头像URL */
    @TableField("avatar")
    private String avatar;

    /** Agent描述 */
    @TableField("description")
    private String description;

    /** Agent系统提示词 */
    @TableField("system_prompt")
    private String systemPrompt;

    /** 欢迎消息 */
    @TableField("welcome_message")
    private String welcomeMessage;

    /** Agent可使用的工具列表 */
    @TableField(value = "tool_ids", typeHandler = ListStringConverter.class)
    private List<String> toolIds;

    /** 关联的知识库ID列表 */
    @TableField(value = "knowledge_base_ids", exist = false)
    private List<String> knowledgeBaseIds;

    /** 当前发布的版本ID */
    @TableField("published_version")
    private String publishedVersion;

    /** Agent状态：1-启用，0-禁用 */
    @TableField("enabled")
    private Boolean enabled;



    /** 创建者用户ID */
    @TableField("user_id")
    private String userId;

    /** 预先设置工具参数，结构如下： { "<mcpServerName>":{ "toolName":"paranms" } } */
    @TableField(value = "tool_preset_params", typeHandler = MapConverter.class)
    private Map<String, Map<String, Map<String, String>>> toolPresetParams;

    /** 是否支持多模态 */
    @TableField("multi_modal")
    private Boolean multiModal;

    /** 无参构造函数 */
    public AgentEntity() {
        this.toolIds = new ArrayList<>();
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

    public List<String> getToolIds() {
        return toolIds != null ? toolIds : new ArrayList<>();
    }

    public void setToolIds(List<String> toolIds) {
        this.toolIds = toolIds;
    }

    public List<String> getKnowledgeBaseIds() {
        return knowledgeBaseIds != null ? knowledgeBaseIds : new ArrayList<>();
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

    /** 创建新的Agent对象 */
    public static AgentEntity createNew(String name, String description, String avatar, String userId) {
        AgentEntity agent = new AgentEntity();
        agent.setName(name);
        agent.setDescription(description);
        agent.setAvatar(avatar);
        agent.setUserId(userId);
        agent.setEnabled(true); // 默认启用
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        return agent;
    }

    /** 更新Agent基本信息 */
    public void updateBasicInfo(String name, String avatar, String description) {
        this.name = name;
        this.avatar = avatar;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /** 启用Agent */
    public void enable() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** 禁用Agent */
    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /** 发布新版本 */
    public void publishVersion(String versionId) {
        this.publishedVersion = versionId;
        this.updatedAt = LocalDateTime.now();
    }

    /** 软删除 */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }



    public void isEnable() {
        if (!this.enabled) {
            throw new BusinessException("助理未激活");
        }
    }

    /** 获取预先设置的工具参数 */
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
}