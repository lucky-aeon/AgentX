package org.xhy.application.conversation.service.message.agent.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.agent.handler.AbstractAgentHandler;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent事件总线
 * 负责注册和分发事件处理器
 */
@Component
public class AgentEventBus {
    private static final Logger log = LoggerFactory.getLogger(AgentEventBus.class);
    private static final Map<AgentWorkflowState, List<AgentEventHandler>> handlers = new ConcurrentHashMap<>();
    
    /**
     * 注册事件处理器
     * 
     * @param state 关注的状态
     * @param handler 处理器实例
     */
    public static void register(AgentWorkflowState state, AgentEventHandler handler) {
        handlers.computeIfAbsent(state, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
    
    /**
     * 发布事件
     * 
     * @param event 工作流事件
     */
    public static void publish(AgentWorkflowEvent event) {
        List<AgentEventHandler> stateHandlers = handlers.getOrDefault(event.getToState(), Collections.emptyList());
        for (AgentEventHandler handler : stateHandlers) {
            try {
                if (((AbstractAgentHandler) handler).getBreak()) {
                    return;
                }
                handler.handle(event);
            } catch (Exception e) {
                // 处理事件处理异常
                log.error("Error handling event: ", e);
            }
        }
    }
} 