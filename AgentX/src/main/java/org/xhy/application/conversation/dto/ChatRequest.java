package org.xhy.application.conversation.dto;


import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求DTO
 */
public class ChatRequest {

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不可为空")
    private String message;
    
    /**
     * 会话ID
     */
    @NotBlank(message = "会话id不可为空")
    private String sessionId;

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
}
