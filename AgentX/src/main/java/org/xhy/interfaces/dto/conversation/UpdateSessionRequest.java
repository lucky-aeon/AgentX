package org.xhy.interfaces.dto.conversation;

/**
 * 更新会话的请求对象
 */
public class UpdateSessionRequest {
    
    private String title;
    private String description;
    
    // 构造方法
    public UpdateSessionRequest() {
    }
    
    public UpdateSessionRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    // Getter和Setter
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
} 