package org.xhy.domain.trace.event;

import org.springframework.context.ApplicationEvent;
import org.xhy.domain.trace.model.TraceContext;

import java.time.LocalDateTime;

/** 执行开始事件 */
public class ExecutionStartedEvent extends ApplicationEvent {

    private final TraceContext traceContext;
    private final String userMessage;
    private final String messageType;
    private final LocalDateTime userMessageTime;

    public ExecutionStartedEvent(Object source, TraceContext traceContext, String userMessage, String messageType) {
        super(source);
        this.traceContext = traceContext;
        this.userMessage = userMessage;
        this.messageType = messageType;
        this.userMessageTime = LocalDateTime.now(); // 默认使用当前时间
    }

    public ExecutionStartedEvent(Object source, TraceContext traceContext, String userMessage, String messageType, LocalDateTime userMessageTime) {
        super(source);
        this.traceContext = traceContext;
        this.userMessage = userMessage;
        this.messageType = messageType;
        this.userMessageTime = userMessageTime;
    }

    public TraceContext getTraceContext() {
        return traceContext;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getMessageType() {
        return messageType;
    }

    public LocalDateTime getUserMessageTime() {
        return userMessageTime;
    }
}