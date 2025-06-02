package org.xhy.application.conversation.dto;

import java.util.List;
import java.util.Map;

/**
 * Agent预览请求DTO
 * 用于预览尚未创建的Agent的对话效果
 */
public class AgentPreviewRequest {

    /** 用户当前输入的消息 */
    private String userMessage;

    /** 系统提示词 */
    private String systemPrompt;

    /** 工具ID列表 */
    private List<String> toolIds;

    /** 工具预设参数 */
    private Map<String, Map<String, Map<String, String>>> toolPresetParams;

    /** 历史消息上下文 */
    private List<MessageDTO> messageHistory;

    /** 使用的模型ID，如果为空则使用用户默认模型 */
    private String modelId;

    /**
     * 文件列表
     */
    private List<String> fileUrls;

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<String> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<String> toolIds) {
        this.toolIds = toolIds;
    }

    public Map<String, Map<String, Map<String, String>>> getToolPresetParams() {
        return toolPresetParams;
    }

    public void setToolPresetParams(Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        this.toolPresetParams = toolPresetParams;
    }

    public List<MessageDTO> getMessageHistory() {
        return messageHistory;
    }

    public void setMessageHistory(List<MessageDTO> messageHistory) {
        this.messageHistory = messageHistory;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
} 