package org.xhy.application.conversation.service.message.agent.event;

/**
 * Agent事件处理器接口
 * 所有状态处理器都需要实现此接口
 */
public interface AgentEventHandler {
    /**
     * 处理工作流事件
     * 
     * @param event 工作流事件
     */
    void handle(AgentWorkflowEvent event);
} 