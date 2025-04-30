package org.xhy.application.conversation.dto;

import java.util.List;
import org.xhy.application.task.dto.TaskDTO;
import org.xhy.domain.conversation.constant.MessageType;

/** 流式聊天响应DTO */
public class AgentChatResponse {

    /** 响应内容片段 */
    private String content;

    /** 是否是最后一个片段 */
    private boolean done;

    /** 消息类型 */
    private MessageType messageType = MessageType.TEXT;

    /** 关联的任务ID（可选） */
    private String taskId;

    /** 数据载荷，用于传递非文本内容 */
    private String payload;

    /** 时间戳 */
    private Long timestamp = System.currentTimeMillis();

    /** 任务列表，用于TASK_IDS类型消息 */
    private List<TaskDTO> tasks;

    public AgentChatResponse() {
    }

    public static AgentChatResponse buildEndMessage(MessageType messageType) {

        AgentChatResponse streamChatResponse = new AgentChatResponse();
        streamChatResponse.setContent("");
        streamChatResponse.setDone(true);
        streamChatResponse.setMessageType(messageType);
        return streamChatResponse;
    }

    public static AgentChatResponse buildEndMessage(String content, MessageType messageType) {

        AgentChatResponse streamChatResponse = new AgentChatResponse();
        streamChatResponse.setContent(content);
        streamChatResponse.setDone(true);
        streamChatResponse.setMessageType(messageType);
        return streamChatResponse;
    }

    public static AgentChatResponse build(String content, MessageType messageType) {

        AgentChatResponse streamChatResponse = new AgentChatResponse();
        streamChatResponse.setContent(content);
        streamChatResponse.setDone(false);
        streamChatResponse.setMessageType(messageType);
        return streamChatResponse;
    }

    public AgentChatResponse(String content, boolean done) {
        this.content = content;
        this.done = done;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDTO> tasks) {
        this.tasks = tasks;
    }
}
