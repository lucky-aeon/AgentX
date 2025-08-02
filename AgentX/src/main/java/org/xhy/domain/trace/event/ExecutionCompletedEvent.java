package org.xhy.domain.trace.event;

import org.springframework.context.ApplicationEvent;
import org.xhy.domain.trace.constant.ExecutionPhase;
import org.xhy.domain.trace.model.TraceContext;

/** 执行完成事件 */
public class ExecutionCompletedEvent extends ApplicationEvent {

    private final TraceContext traceContext;
    private final boolean success;
    private final ExecutionPhase errorPhase;
    private final String errorMessage;

    public ExecutionCompletedEvent(Object source, TraceContext traceContext, boolean success, ExecutionPhase errorPhase,
            String errorMessage) {
        super(source);
        this.traceContext = traceContext;
        this.success = success;
        this.errorPhase = errorPhase;
        this.errorMessage = errorMessage;
    }

    public TraceContext getTraceContext() {
        return traceContext;
    }

    public boolean isSuccess() {
        return success;
    }

    public ExecutionPhase getErrorPhase() {
        return errorPhase;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}