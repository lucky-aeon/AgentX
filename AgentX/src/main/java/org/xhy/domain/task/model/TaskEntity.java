package org.xhy.domain.task.model;

import com.baomidou.mybatisplus.annotation.*;
import org.xhy.domain.task.constant.TaskStatus;
import org.xhy.infrastructure.converter.ScheduledTaskStatusConverter;
import org.xhy.infrastructure.entity.BaseEntity;

import java.time.LocalDateTime;

/** 任务实体类 */
@TableName("agent_tasks")
public class TaskEntity extends BaseEntity {
    /** 任务ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属会话ID */
    @TableField("session_id")
    private String sessionId;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 父任务ID */
    @TableField("parent_task_id")
    private String parentTaskId;

    /** 任务名称 */
    @TableField("task_name")
    private String taskName;

    /** 任务描述 */
    @TableField("description")
    private String description;

    /** 任务状态，只有子任务有 */
    @TableField(value = "status", typeHandler = ScheduledTaskStatusConverter.class)
    private TaskStatus status;

    /** 任务进度,存放父任务中 */
    @TableField("progress")
    private Integer progress = 0;

    /** 开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 任务结果 */
    @TableField("task_result")
    private String taskResult;

    public TaskEntity() {
        // 默认构造函数
    }

    /** 更新任务状态
     *
     * @param status 新状态 */
    public void updateStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.IN_PROGRESS && this.startTime == null) {
            this.startTime = LocalDateTime.now();
        } else if ((status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) && this.endTime == null) {
            this.endTime = LocalDateTime.now();
        }

        if (status == TaskStatus.COMPLETED) {
            this.progress = 100;
        }
    }

    /** 更新进度
     *
     * @param progress 进度值(0-100) */
    public void updateProgress(Integer progress) {
        if (progress < 0) {
            this.progress = 0;
        } else if (progress > 100) {
            this.progress = 100;
        } else {
            this.progress = progress;
        }
    }

    // Getters and Setters

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    /** 获取任务结果
     * 
     * @return 任务结果 */
    public String getTaskResult() {
        return taskResult;
    }

    /** 设置任务结果
     * 
     * @param taskResult 任务结果 */
    public void setTaskResult(String taskResult) {
        this.taskResult = taskResult;
    }
}