package org.xhy.domain.llm.model;

/**
 * LLM消息模型
 */
public class LlmMessage {
    
    /**
     * 消息角色
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    public LlmMessage() {
    }
    
    public LlmMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static LlmMessage ofUser(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage ofAssistant(String content) {
        return new LlmMessage("assistant", content);
    }

    public static LlmMessage ofSystem(String content) {
        return new LlmMessage("system", content);
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}
