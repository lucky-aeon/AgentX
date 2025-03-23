package org.xhy.interfaces.dto.conversation;

/**
 * 创建会话并发送消息的请求对象
 */
public class CreateAndChatRequest {
    
    private String title;
    private String userId;
    private String content;
    
    // 构造方法
    public CreateAndChatRequest() {
    }
    
    public CreateAndChatRequest(String title, String userId, String content) {
        this.title = title;
        this.userId = userId;
        this.content = content;
    }
    
    // Getter和Setter
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
} 