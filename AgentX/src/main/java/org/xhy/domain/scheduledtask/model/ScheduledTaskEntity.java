package org.xhy.domain.scheduledtask.model;

import com.baomidou.mybatisplus.annotation.*;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.infrastructure.converter.RepeatConfigConverter;
import org.xhy.infrastructure.converter.RepeatTypeConverter;
import org.xhy.infrastructure.converter.ScheduledTaskStatusConverter;
import org.xhy.infrastructure.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

/** 定时任务实体类 代表一个用户创建的定时任务 */
@TableName(value = "scheduled_tasks", autoResultMap = true)
public class ScheduledTaskEntity extends BaseEntity {

    /** 定时任务唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 关联的Agent ID */
    @TableField("agent_id")
    private String agentId;

    /** 关联的会话ID */
    @TableField("session_id")
    private String sessionId;

    /** 任务内容 */
    @TableField("content")
    private String content;

    /** 重复类型 */
    @TableField(value = "repeat_type", typeHandler = RepeatTypeConverter.class)
    private RepeatType repeatType;

    /** 重复配置，JSON格式 */
    @TableField(value = "repeat_config", typeHandler = RepeatConfigConverter.class)
    private RepeatConfig repeatConfig;

    /** 任务状态 */
    @TableField(value = "status", typeHandler = ScheduledTaskStatusConverter.class)
    private ScheduleTaskStatus status;

    /** 上次执行时间 */
    @TableField("last_execute_time")
    private LocalDateTime lastExecuteTime;

    /** 下次执行时间 */
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    /** 无参构造函数 */
    public ScheduledTaskEntity() {
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RepeatType getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(RepeatType repeatType) {
        this.repeatType = repeatType;
    }

    public RepeatConfig getRepeatConfig() {
        return repeatConfig;
    }

    public void setRepeatConfig(RepeatConfig repeatConfig) {
        this.repeatConfig = repeatConfig;
    }

    public ScheduleTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleTaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastExecuteTime() {
        return lastExecuteTime;
    }

    public void setLastExecuteTime(LocalDateTime lastExecuteTime) {
        this.lastExecuteTime = lastExecuteTime;
    }

    public LocalDateTime getNextExecuteTime() {
        return nextExecuteTime;
    }

    public void setNextExecuteTime(LocalDateTime nextExecuteTime) {
        this.nextExecuteTime = nextExecuteTime;
    }

    /** 创建新的定时任务 */
    public static ScheduledTaskEntity createNew(String userId, String agentId, String sessionId, String content,
            RepeatType repeatType, RepeatConfig repeatConfig) {
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setUserId(userId);
        task.setAgentId(agentId);
        task.setSessionId(sessionId);
        task.setContent(content);
        task.setRepeatType(repeatType);
        task.setRepeatConfig(repeatConfig);
        task.setStatus(ScheduleTaskStatus.ACTIVE);
        return task;
    }

    /** 更新任务内容 */
    public void updateContent(String content) {
        this.content = content;
    }

    /** 更新重复配置 */
    public void updateRepeatConfig(RepeatType repeatType, RepeatConfig repeatConfig) {
        this.repeatType = repeatType;
        this.repeatConfig = repeatConfig;
    }

    /** 暂停任务 */
    public void pause() {
        this.status = ScheduleTaskStatus.PAUSED;
    }

    /** 恢复任务 */
    public void resume() {
        this.status = ScheduleTaskStatus.ACTIVE;
    }

    /** 完成任务 */
    public void complete() {
        this.status = ScheduleTaskStatus.COMPLETED;
    }

    /** 记录执行时间 */
    public void recordExecution() {
        this.lastExecuteTime = LocalDateTime.now();
    }

    /** 检查任务是否活跃 */
    public boolean isActive() {
        return ScheduleTaskStatus.ACTIVE.equals(this.status);
    }

    /** 检查任务是否暂停 */
    public boolean isPaused() {
        return ScheduleTaskStatus.PAUSED.equals(this.status);
    }

    /** 检查任务是否完成 */
    public boolean isCompleted() {
        return ScheduleTaskStatus.COMPLETED.equals(this.status);
    }

    /** 检查是否为一次性任务 */
    public boolean isOneTime() {
        return RepeatType.NONE.equals(this.repeatType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScheduledTaskEntity that = (ScheduledTaskEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ScheduledTaskEntity{" + "id='" + id + '\'' + ", userId='" + userId + '\'' + ", agentId='" + agentId
                + '\'' + ", sessionId='" + sessionId + '\'' + ", content='" + content + '\'' + ", repeatType="
                + repeatType + ", status=" + status + ", lastExecuteTime=" + lastExecuteTime + ", nextExecuteTime="
                + nextExecuteTime + '}';
    }
}