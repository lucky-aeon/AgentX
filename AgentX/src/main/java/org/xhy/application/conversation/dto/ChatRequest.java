package org.xhy.application.conversation.dto;

import javax.validation.constraints.NotBlank;

/**
 * 聊天请求DTO
 */
public class ChatRequest {
    
    /**
     * 用户输入的消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    /**
     * 会话ID，可选
     */
    private String sessionId;
    
    /**
     * 使用的服务商，可选
     */
    private String provider;
    
    /**
     * 使用的模型，可选
     */
    private String model;
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
}
