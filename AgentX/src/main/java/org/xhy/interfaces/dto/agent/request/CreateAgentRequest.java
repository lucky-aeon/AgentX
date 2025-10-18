package org.xhy.interfaces.dto.agent.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/** 创建Agent的请求对象 */
public class CreateAgentRequest {

    @NotBlank(message = "助理名称不可为空")
    private String name;
    private String description;
    private String avatar;

    private String systemPrompt;
    private String welcomeMessage;
    private List<String> toolIds;
    private List<String> knowledgeBaseIds;
    private List<String> linkedAgentIds;
    private Map<String, Map<String, Map<String, String>>> toolPresetParams;
    private Boolean multiModal;
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

    public List<String> getLinkedAgentIds() {
        return linkedAgentIds;
    }

    public void setLinkedAgentIds(List<String> linkedAgentIds) {
        this.linkedAgentIds = linkedAgentIds;
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
}
