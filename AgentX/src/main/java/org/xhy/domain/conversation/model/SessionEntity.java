package org.xhy.domain.conversation.model;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import org.xhy.infrastructure.entity.BaseEntity;

/** 会话实体类，代表一个独立的对话会话/主题 */
@TableName("sessions")
public class SessionEntity extends BaseEntity {

    /** 会话唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 会话标题 */
    @TableField("title")
    private String title;

    /** 所属用户ID */
    @TableField("user_id")
    private String userId;

    /** 关联的Agent版本ID */
    @TableField("agent_id")
    private String agentId;

    /** 会话描述 */
    @TableField("description")
    private String description;

    /** 是否归档 */
    @TableField("is_archived")
    private boolean isArchived;

    /** 会话元数据，可存储其他自定义信息 */
    @TableField("metadata")
    private String metadata;

    /** 无参构造函数 */
    public SessionEntity() {
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

    /** 创建新会话 */
    public static SessionEntity createNew(String title, String userId) {
        SessionEntity session = new SessionEntity();
        session.setTitle(title);
        session.setUserId(userId);
        session.setArchived(false);
        return session;
    }

    /** 更新会话信息 */
    public void update(String title, String description) {
        this.title = title;
        this.description = description;
        // 自动填充会处理更新时间
    }

    /** 归档会话 */
    public void archive() {
        this.isArchived = true;
        // 自动填充会处理更新时间
    }

    /** 恢复已归档会话 */
    public void unarchive() {
        this.isArchived = false;
        this.updatedAt = LocalDateTime.now();
    }
}
