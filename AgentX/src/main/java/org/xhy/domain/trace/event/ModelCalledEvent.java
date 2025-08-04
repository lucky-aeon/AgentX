package org.xhy.domain.trace.event;

import org.springframework.context.ApplicationEvent;
import org.xhy.domain.trace.model.ModelCallInfo;
import org.xhy.domain.trace.model.TraceContext;

import java.time.LocalDateTime;

/** 模型调用事件 */
public class ModelCalledEvent extends ApplicationEvent {

    private final TraceContext traceContext;
    private final String aiResponse;
    private final ModelCallInfo modelCallInfo;
    private final LocalDateTime aiResponseStartTime;

    public ModelCalledEvent(Object source, TraceContext traceContext, String aiResponse, ModelCallInfo modelCallInfo) {
        super(source);
        this.traceContext = traceContext;
        this.aiResponse = aiResponse;
        this.modelCallInfo = modelCallInfo;
        this.aiResponseStartTime = LocalDateTime.now(); // 默认使用当前时间
    }

    public ModelCalledEvent(Object source, TraceContext traceContext, String aiResponse, ModelCallInfo modelCallInfo, LocalDateTime aiResponseStartTime) {
        super(source);
        this.traceContext = traceContext;
        this.aiResponse = aiResponse;
        this.modelCallInfo = modelCallInfo;
        this.aiResponseStartTime = aiResponseStartTime;
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

    public LocalDateTime getAiResponseStartTime() {
        return aiResponseStartTime;
    }
}