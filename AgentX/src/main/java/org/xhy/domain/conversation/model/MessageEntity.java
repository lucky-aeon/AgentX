package org.xhy.domain.conversation.model;

import com.baomidou.mybatisplus.annotation.*;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.infrastructure.converter.ListConverter;
import org.xhy.infrastructure.converter.MessageTypeConverter;
import org.xhy.infrastructure.converter.RoleConverter;
import org.xhy.infrastructure.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 消息实体类，代表对话中的一条消息 */
@TableName("messages")
public class MessageEntity extends BaseEntity {

    /** 消息唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属会话ID */
    @TableField("session_id")
    private String sessionId;

    /** 消息角色 (user, assistant, system) */
    @TableField(value = "role", typeHandler = RoleConverter.class)
    private Role role;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 消息类型 */
    @TableField(value = "message_type", typeHandler = MessageTypeConverter.class)
    private MessageType messageType = MessageType.TEXT;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** Token数量 */
    @TableField("token_count")
    private Integer tokenCount = 0;

    /** 服务提供商 */
    @TableField("provider")
    private String provider;

    /** 使用的模型 */
    @TableField("model")
    private String model;

    /** 消息元数据 */
    @TableField("metadata")
    private String metadata;

    @TableField(value = "file_urls", typeHandler = ListConverter.class)
    private List<String> fileUrls = new ArrayList<>();

    /** 无参构造函数 */
    public MessageEntity() {
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
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

    public boolean isUserMessage() {
        return this.role == Role.USER;
    }

    public boolean isAIMessage() {
        return this.role == Role.ASSISTANT;
    }

    public boolean isSystemMessage() {
        return this.role == Role.SYSTEM;
    }

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }
}