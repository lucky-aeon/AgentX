package org.xhy.application.conversation.dto;

/**
 * 流式聊天响应DTO
 */
public class StreamChatResponse {
    
    /**
     * 响应内容片段
     */
    private String content;
    
    /**
     * 是否是最后一个片段
     */
    private boolean done;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 使用的服务商
     */
    private String provider;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 时间戳
     */
    private Long timestamp = System.currentTimeMillis();
    
    public StreamChatResponse() {
    }
    
    public StreamChatResponse(String content, boolean done) {
        this.content = content;
        this.done = done;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public void setDone(boolean done) {
        this.done = done;
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
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
} 