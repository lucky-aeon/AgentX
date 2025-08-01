package org.xhy.domain.trace.event;

import org.springframework.context.ApplicationEvent;
import org.xhy.domain.trace.model.TraceContext;

/**
 * 执行开始事件
 */
public class ExecutionStartedEvent extends ApplicationEvent {
    
    private final TraceContext traceContext;
    private final String userMessage;
    private final String messageType;

    public ExecutionStartedEvent(Object source, TraceContext traceContext, String userMessage, String messageType) {
        super(source);
        this.traceContext = traceContext;
        this.userMessage = userMessage;
        this.messageType = messageType;
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
}