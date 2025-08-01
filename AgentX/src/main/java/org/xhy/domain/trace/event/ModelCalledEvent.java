package org.xhy.domain.trace.event;

import org.springframework.context.ApplicationEvent;
import org.xhy.domain.trace.model.ModelCallInfo;
import org.xhy.domain.trace.model.TraceContext;

/**
 * 模型调用事件
 */
public class ModelCalledEvent extends ApplicationEvent {
    
    private final TraceContext traceContext;
    private final String aiResponse;
    private final ModelCallInfo modelCallInfo;

    public ModelCalledEvent(Object source, TraceContext traceContext, String aiResponse, ModelCallInfo modelCallInfo) {
        super(source);
        this.traceContext = traceContext;
        this.aiResponse = aiResponse;
        this.modelCallInfo = modelCallInfo;
    }

    public TraceContext getTraceContext() {
        return traceContext;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public ModelCallInfo getModelCallInfo() {
        return modelCallInfo;
    }
}