package org.xhy.domain.trace.event;

import org.springframework.context.ApplicationEvent;
import org.xhy.domain.trace.model.ToolCallInfo;
import org.xhy.domain.trace.model.TraceContext;

/** 工具执行事件 */
public class ToolExecutedEvent extends ApplicationEvent {

    private final TraceContext traceContext;
    private final ToolCallInfo toolCallInfo;

    public ToolExecutedEvent(Object source, TraceContext traceContext, ToolCallInfo toolCallInfo) {
        super(source);
        this.traceContext = traceContext;
        this.toolCallInfo = toolCallInfo;
    }

    public TraceContext getTraceContext() {
        return traceContext;
    }

    public ToolCallInfo getToolCallInfo() {
        return toolCallInfo;
    }
}