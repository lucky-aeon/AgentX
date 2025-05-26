package org.xhy.interfaces.dto.scheduledtask.request;

import jakarta.validation.constraints.NotBlank;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.RepeatConfig;

/** 更新定时任务请求 */
public class UpdateScheduledTaskRequest {

    /** 任务ID */
    @NotBlank(message = "任务ID不能为空")
    private String id;

    /** 任务内容 */
    private String content;

    /** 重复类型 */
    private RepeatType repeatType;

    /** 重复配置 */
    private RepeatConfig repeatConfig;

    /** 任务状态 */
    private ScheduleTaskStatus status;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}