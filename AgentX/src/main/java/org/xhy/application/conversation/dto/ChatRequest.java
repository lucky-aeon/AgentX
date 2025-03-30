package org.xhy.application.conversation.dto;


/**
 * 聊天请求DTO
 */
public class ChatRequest {

    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 会话ID，可选
     */
    private String sessionId;

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
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
}
