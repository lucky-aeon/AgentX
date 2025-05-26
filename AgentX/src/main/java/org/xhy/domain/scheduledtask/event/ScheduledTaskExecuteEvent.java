package org.xhy.domain.scheduledtask.event;

import org.springframework.context.ApplicationEvent;

/** 定时任务执行事件 当定时任务需要执行时，Domain层发布此事件 */
public class ScheduledTaskExecuteEvent extends ApplicationEvent {

    private final String taskId;
    private final String userId;
    private final String sessionId;
    private final String content;

    public ScheduledTaskExecuteEvent(Object source, String taskId, String userId, String sessionId, String content) {
        super(source);
        this.taskId = taskId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.content = content;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ScheduledTaskExecuteEvent{" + "taskId='" + taskId + '\'' + ", userId='" + userId + '\''
                + ", sessionId='" + sessionId + '\'' + ", content='" + content + '\'' + '}';
    }
}