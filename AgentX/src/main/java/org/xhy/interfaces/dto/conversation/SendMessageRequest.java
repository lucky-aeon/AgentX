package org.xhy.interfaces.dto.conversation;

/**
 * 发送消息的请求对象
 */
public class SendMessageRequest {
    
    private String content;
    
    // 构造方法
    public SendMessageRequest() {
    }
    
    public SendMessageRequest(String content) {
        this.content = content;
    }
    
    // Getter和Setter
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
} 