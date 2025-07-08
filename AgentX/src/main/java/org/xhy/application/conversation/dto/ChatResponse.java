package org.xhy.application.conversation.dto;

/** 聊天响应DTO */
public class ChatResponse {

    /** 响应内容 */
    private String content;

    /** 会话ID */
    private String sessionId;

    /** 使用的服务商 */
    private String provider;

    /** 使用的模型 */
    private String model;

    /** 创建时间 */
    private Long timestamp = System.currentTimeMillis();

    public ChatResponse() {
    }

    public ChatResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
