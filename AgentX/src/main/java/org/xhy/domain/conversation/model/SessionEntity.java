package org.xhy.domain.conversation.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.time.LocalDateTime;

/**
 * 会话实体类，代表一个独立的对话会话/主题
 */
@TableName("sessions")
public class SessionEntity extends Model<SessionEntity> {

    /**
     * 会话唯一ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 会话标题
     */
    @TableField("title")
    private String title;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 关联的Agent版本ID
     */
    @TableField("agent_id")
    private String agentId;

    /**
     * 会话描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否归档
     */
    @TableField("is_archived")
    private boolean isArchived;

    /**
     * 会话元数据，可存储其他自定义信息
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 无参构造函数
     */
    public SessionEntity() {
    }

    /**
     * 全参构造函数
     */
    public SessionEntity(String id, String title, String userId, String agentVersionId, String description,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            boolean isArchived, String metadata) {
        this.id = id;
        this.title = title;
        this.userId = userId;
        this.agentId = agentVersionId;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isArchived = isArchived;
        this.metadata = metadata;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentVersionId) {
        this.agentId = agentVersionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 创建新会话
     */
    public static SessionEntity createNew(String title, String userId) {
        SessionEntity session = new SessionEntity();
        session.setTitle(title);
        session.setUserId(userId);
        session.setArchived(false);
        return session;
    }

    /**
     * 更新会话信息
     */
    public void update(String title, String description) {
        this.title = title;
        this.description = description;
        // 自动填充会处理更新时间
    }

    /**
     * 归档会话
     */
    public void archive() {
        this.isArchived = true;
        // 自动填充会处理更新时间
    }

    /**
     * 恢复已归档会话
     */
    public void unarchive() {
        this.isArchived = false;
        this.updatedAt = LocalDateTime.now();
    }

}