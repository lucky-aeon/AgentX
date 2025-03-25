package org.xhy.domain.agent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Agent工作区实体类
 * 用于记录用户添加到工作区的Agent
 */
@TableName("agent_workspace")
public class AgentWorkspaceEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * Agent ID
     */
    @TableField("agent_id")
    private String agentId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 默认构造函数
     */
    public AgentWorkspaceEntity() {
    }

    /**
     * 带参数的构造函数
     *
     * @param id        主键ID
     * @param agentId   Agent ID
     * @param userId    用户ID
     * @param createdAt 创建时间
     */
    public AgentWorkspaceEntity(String id, String agentId, String userId, LocalDateTime createdAt) {
        this.id = id;
        this.agentId = agentId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // Getter 和 Setter 方法

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
