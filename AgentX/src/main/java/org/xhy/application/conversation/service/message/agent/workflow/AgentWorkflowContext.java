package org.xhy.application.conversation.service.message.agent.workflow;

import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.application.conversation.service.message.agent.event.AgentEventBus;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent工作流上下文
 * 维护工作流执行过程中的状态和数据
 */
public class AgentWorkflowContext<T> {
    // 工作流上下文的唯一标识符
    private final String id = UUID.randomUUID().toString();

    // 对话上下文，包含用户信息、会话信息、模型配置等
    private ChatContext chatContext;

    // 消息传输接口，用于向前端发送消息
    private MessageTransport<T> messageTransport;

    // 与前端的连接对象，用于流式传输数据
    private T connection;

    // 用户发送的消息实体
    private MessageEntity userMessageEntity;

    // 大模型返回的消息实体，任务拆分结果会保存在这里
    private MessageEntity llmMessageEntity;

    // 父任务实体，代表整个复杂任务
    private TaskEntity parentTask;

    // 当前工作流状态，使用volatile保证多线程可见性
    private volatile AgentWorkflowState state = AgentWorkflowState.INITIALIZED;

    // 子任务映射，键为任务描述，值为任务实体
    private final Map<String, TaskEntity> subTaskMap = new HashMap<>();

    // 所有子任务描述的有序列表
    private final List<String> tasks = new ArrayList<>();

    // 已完成子任务的结果，键为任务描述，值为执行结果
    private final Map<String, String> taskResults = new LinkedHashMap<>();

    // 已完成的子任务数量
    private int completedTaskCount = 0;

    // 当前正在执行的子任务索引，使用AtomicInteger保证线程安全
    private AtomicInteger currentTaskIndex = new AtomicInteger(0);

    private Map<String,Object> extraData = new HashMap<>();


    /**
     * 转换状态并发布事件
     */
    public void transitionTo(AgentWorkflowState newState) {
        AgentWorkflowState oldState = this.state;
        this.state = newState;
        AgentWorkflowEvent event = new AgentWorkflowEvent(this, oldState, newState);
        AgentEventBus.publish(event);
    }
    
    /**
     * 发送消息到前端
     */
    public void sendMessage(String content, MessageType messageType) {
        AgentChatResponse response = AgentChatResponse.build(content, messageType);
        messageTransport.sendMessage(connection, response);
    }
    
    /**
     * 发送终止消息到前端
     */
    public void sendEndMessage(String content, MessageType messageType) {
        AgentChatResponse response = AgentChatResponse.buildEndMessage(content, messageType);
        messageTransport.sendMessage(connection, response);
    }
    
    /**
     * 发送终止消息到前端（无内容）
     */
    public void sendEndMessage(MessageType messageType) {
        AgentChatResponse response = AgentChatResponse.buildEndMessage(messageType);
        messageTransport.sendMessage(connection, response);
    }
    /**
     * 发送终止消息到前端（无内容）
     */
    public void sendEndWithTaskIdMessage(String taskId,MessageType messageType) {
        AgentChatResponse response = AgentChatResponse.buildEndMessage(messageType);
        response.setTaskId(taskId);
        messageTransport.sendMessage(connection, response);
    }
    
    /**
     * 处理错误
     */
    public void handleError(Throwable error) {
        String errorMessage = "任务执行过程中发生错误: " + error.getMessage();
        sendEndMessage(errorMessage, MessageType.TEXT);
        messageTransport.handleError(connection, error);
        transitionTo(AgentWorkflowState.FAILED);
    }
    
    /**
     * 添加子任务
     */
    public void addSubTask(String taskName, TaskEntity taskEntity) {
        tasks.add(taskName);
        subTaskMap.put(taskName, taskEntity);
    }
    
    /**
     * 添加任务结果
     */
    public void addTaskResult(String taskName, String result) {
        taskResults.put(taskName, result);
        completedTaskCount++;
    }
    
    /**
     * 是否所有任务都已完成
     */
    public boolean areAllTasksCompleted() {
        return completedTaskCount >= tasks.size();
    }
    
    /**
     * 获取下一个要执行的任务
     */
    public String getNextTask() {
        int index = currentTaskIndex.getAndIncrement();
        if (index < tasks.size()) {
            return tasks.get(index);
        }
        return null;
    }
    
    /**
     * 是否还有下一个任务
     */
    public boolean hasNextTask() {
        return currentTaskIndex.get() < tasks.size();
    }
    
    /**
     * 构建任务结果汇总文本
     */
    public String buildTaskSummary() {
        StringBuilder taskSummaryBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : taskResults.entrySet()) {
            taskSummaryBuilder.append("任务: ").append(entry.getKey())
                    .append("\n结果: ").append(entry.getValue())
                    .append("\n\n");
        }
        return taskSummaryBuilder.toString();
    }
    
    /**
     * 完成连接
     */
    public void completeConnection() {
        messageTransport.completeConnection(connection);
    }

    // Getters and Setters
    
    public String getId() {
        return id;
    }

    public ChatContext getChatContext() {
        return chatContext;
    }

    public void setChatContext(ChatContext chatContext) {
        this.chatContext = chatContext;
    }

    public MessageTransport<T> getMessageTransport() {
        return messageTransport;
    }

    public void setMessageTransport(MessageTransport<T> messageTransport) {
        this.messageTransport = messageTransport;
    }

    public T getConnection() {
        return connection;
    }

    public void setConnection(T connection) {
        this.connection = connection;
    }

    public MessageEntity getUserMessageEntity() {
        return userMessageEntity;
    }

    public void setUserMessageEntity(MessageEntity userMessageEntity) {
        this.userMessageEntity = userMessageEntity;
    }

    public MessageEntity getLlmMessageEntity() {
        return llmMessageEntity;
    }

    public void setLlmMessageEntity(MessageEntity llmMessageEntity) {
        this.llmMessageEntity = llmMessageEntity;
    }

    public TaskEntity getParentTask() {
        return parentTask;
    }

    public void setParentTask(TaskEntity parentTask) {
        this.parentTask = parentTask;
    }

    public AgentWorkflowState getState() {
        return state;
    }

    public Map<String, TaskEntity> getSubTaskMap() {
        return subTaskMap;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public Map<String, String> getTaskResults() {
        return taskResults;
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    public int getTotalTaskCount() {
        return tasks.size();
    }

    public void addExtraData(String key, Object value) {
        this.extraData.put(key, value);
    }
    public Object getExtraData(String key) {
        return this.extraData.get(key);
    }
} 