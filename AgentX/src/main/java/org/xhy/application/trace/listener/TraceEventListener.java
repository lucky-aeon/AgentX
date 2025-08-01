package org.xhy.application.trace.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xhy.domain.trace.event.*;

/**
 * 追踪事件监听器
 * 异步处理追踪事件，可用于扩展功能如日志记录、监控等
 */
@Component
public class TraceEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TraceEventListener.class);

    /**
     * 处理执行开始事件
     */
    @Async
    @EventListener
    public void handleExecutionStarted(ExecutionStartedEvent event) {
        try {
            logger.debug("执行开始 - TraceId: {}, SessionId: {}, AgentId: {}", 
                    event.getTraceContext().getTraceId(),
                    event.getTraceContext().getSessionId(),
                    event.getTraceContext().getAgentId());
            
            // 这里可以扩展其他功能，如发送监控指标等
        } catch (Exception e) {
            logger.error("处理执行开始事件失败", e);
        }
    }

    /**
     * 处理模型调用事件
     */
    @Async
    @EventListener
    public void handleModelCalled(ModelCalledEvent event) {
        try {
            logger.debug("模型调用完成 - TraceId: {}, ModelId: {}, InputTokens: {}, OutputTokens: {}", 
                    event.getTraceContext().getTraceId(),
                    event.getModelCallInfo().getModelId(),
                    event.getModelCallInfo().getInputTokens(),
                    event.getModelCallInfo().getOutputTokens());
            
            // 这里可以扩展功能，如成本监控、性能分析等
        } catch (Exception e) {
            logger.error("处理模型调用事件失败", e);
        }
    }

    /**
     * 处理工具执行事件
     */
    @Async
    @EventListener
    public void handleToolExecuted(ToolExecutedEvent event) {
        try {
            logger.debug("工具执行完成 - TraceId: {}, ToolName: {}, Success: {}, ExecutionTime: {}ms", 
                    event.getTraceContext().getTraceId(),
                    event.getToolCallInfo().getToolName(),
                    event.getToolCallInfo().getSuccess(),
                    event.getToolCallInfo().getExecutionTime());
            
            // 这里可以扩展功能，如工具使用统计、异常告警等
        } catch (Exception e) {
            logger.error("处理工具执行事件失败", e);
        }
    }

    /**
     * 处理执行完成事件
     */
    @Async
    @EventListener
    public void handleExecutionCompleted(ExecutionCompletedEvent event) {
        try {
            if (event.isSuccess()) {
                logger.debug("执行完成成功 - TraceId: {}", event.getTraceContext().getTraceId());
            } else {
                logger.warn("执行完成失败 - TraceId: {}, ErrorPhase: {}, ErrorMessage: {}", 
                        event.getTraceContext().getTraceId(),
                        event.getErrorPhase() != null ? event.getErrorPhase().getCode() : "UNKNOWN",
                        event.getErrorMessage());
            }
            
            // 这里可以扩展功能，如失败告警、性能监控等
        } catch (Exception e) {
            logger.error("处理执行完成事件失败", e);
        }
    }
}