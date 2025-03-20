package org.xhy.domain.conversation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.time.LocalDateTime;

/**
 * 消息实体类，代表对话中的一条消息
 */
@TableName("messages")
public class Message extends Model<Message> {

    /**
     * 消息唯一ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 所属会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 消息角色 (user, assistant, system)
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * Token数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 服务提供商
     */
    @TableField("provider")
    private String provider;

    /**
     * 使用的模型
     */
    @TableField("model")
    private String model;

    /**
     * 消息元数据
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 无参构造函数
     */
    public Message() {
    }

    /**
     * 全参构造函数
     */
    public Message(String id, String sessionId, String role, String content,
            LocalDateTime createdAt, Integer tokenCount, String provider,
            String model, String metadata) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.tokenCount = tokenCount;
        this.provider = provider;
        this.model = model;
        this.metadata = metadata;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 创建用户消息
     */
    public static Message createUserMessage(String sessionId, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole("user");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    /**
     * 创建系统消息
     */
    public static Message createSystemMessage(String sessionId, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole("system");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    /**
     * 创建助手消息
     */
    public static Message createAssistantMessage(String sessionId, String content,
            String provider, String model, Integer tokenCount) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole("assistant");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        message.setProvider(provider);
        message.setModel(model);
        message.setTokenCount(tokenCount);
        return message;
    }

    /**
     * 转换为API响应格式
     */
    public MessageDTO toDTO() {
        MessageDTO dto = new MessageDTO();
        dto.setId(this.id);
        dto.setRole(this.role);
        dto.setContent(this.content);
        dto.setCreatedAt(this.createdAt);
        return dto;
    }
}