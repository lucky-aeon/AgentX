package org.xhy.interfaces.dto.agent.request;

import jakarta.validation.constraints.NotBlank;
import org.xhy.domain.agent.model.LLMModelConfig;

import java.util.List;
import java.util.Map;

/** 更新Agent信息的请求对象 整合了基本信息和配置信息 */
public class UpdateAgentRequest {

    private String id;
    @NotBlank(message = "助理名称不可为空")
    private String name;
    private String avatar;
    private String description;
    private Boolean enabled;

    // 配置信息字段
    private String systemPrompt;
    private String welcomeMessage;
    private LLMModelConfig modelConfig;
    private List<String> toolIds;
    private List<String> knowledgeBaseIds;
    private Map<String,Map<String,Map<String,String>>> toolPresetParams;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Map<String,Map<String,Map<String,String>>> getToolPresetParams() {
        return toolPresetParams;
    }

    public void setToolPresetParams(Map<String,Map<String,Map<String,String>>> toolPresetParams) {
        this.toolPresetParams = toolPresetParams;
    }
}