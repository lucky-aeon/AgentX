package org.xhy.interfaces.dto.scheduledtask.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.model.RepeatConfig;

/**
 * 创建定时任务请求
 */
public class CreateScheduledTaskRequest {

    /** Agent ID */
    @NotBlank(message = "Agent ID不能为空")
    private String agentId;

    /** 会话ID */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** 任务内容 */
    @NotBlank(message = "任务内容不能为空")
    private String content;

    /** 重复类型 */
    @NotNull(message = "重复类型不能为空")
    private RepeatType repeatType;

    /** 重复配置 */
    @NotNull(message = "重复配置不能为空")
    private RepeatConfig repeatConfig;

    // Getters and Setters
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
} 