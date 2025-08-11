package org.xhy.application.trace.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xhy.domain.trace.event.*;
import org.xhy.domain.trace.constant.ExecutionPhase;
import org.xhy.domain.trace.service.AgentExecutionTraceDomainService;

import java.time.LocalDateTime;

/** 追踪事件监听器 异步处理追踪事件，可用于扩展功能如日志记录、监控等 */
@Component
public class TraceEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TraceEventListener.class);
    private final AgentExecutionTraceDomainService traceDomainService;

    public TraceEventListener(AgentExecutionTraceDomainService traceDomainService) {
        this.traceDomainService = traceDomainService;
    }

    /** 处理执行开始事件 */
    @EventListener
    public void handleExecutionStarted(ExecutionStartedEvent event) {
        try {
            logger.debug("执行开始 - TraceId: {}, SessionId: {}, AgentId: {}, MessageTime: {}",
                    event.getTraceContext().getTraceId(), event.getTraceContext().getSessionId(),
                    event.getTraceContext().getAgentId(), event.getUserMessageTime());

            // 保存用户消息到数据库，使用事件中的实际时间戳
            traceDomainService.recordUserMessage(event.getTraceContext(), event.getUserMessage(),
                    event.getMessageType(), event.getUserMessageTime());

            logger.debug("用户消息已保存 - TraceId: {}, MessageTime: {}", event.getTraceContext().getTraceId(),
                    event.getUserMessageTime());

        } catch (Exception e) {
            logger.error("处理执行开始事件失败 - TraceId: {}", event.getTraceContext().getTraceId(), e);
        }
    }

    /** 处理模型调用事件 */
    @EventListener
    public void handleModelCalled(ModelCalledEvent event) {
        try {
            logger.debug("模型调用完成 - TraceId: {}, ModelId: {}, InputTokens: {}, OutputTokens: {}, ResponseStartTime: {}",
                    event.getTraceContext().getTraceId(), event.getModelCallInfo().getModelId(),
                    event.getModelCallInfo().getInputTokens(), event.getModelCallInfo().getOutputTokens(),
                    event.getAiResponseStartTime());

            // 用户消息已在 TraceCollector.getOrStartExecution() 中保存，Token数量通过 updateUserMessageTokens() 更新

            // 保存AI响应到数据库，使用事件中的AI响应开始时间
            traceDomainService.recordAiResponse(event.getTraceContext(), event.getAiResponse(),
                    event.getModelCallInfo(), event.getAiResponseStartTime());

            logger.debug("AI响应已保存 - TraceId: {}, Success: {}, ResponseStartTime: {}",
                    event.getTraceContext().getTraceId(), event.getModelCallInfo().getSuccess(),
                    event.getAiResponseStartTime());

        } catch (Exception e) {
            logger.error("处理模型调用事件失败 - TraceId: {}", event.getTraceContext().getTraceId(), e);
        }
    }

    /** 处理工具执行事件 */
    @EventListener
    public void handleToolExecuted(ToolExecutedEvent event) {
        try {
            logger.debug("工具执行完成 - TraceId: {}, ToolName: {}, Success: {}, ExecutionTime: {}ms, StartTime: {}",
                    event.getTraceContext().getTraceId(), event.getToolCallInfo().getToolName(),
                    event.getToolCallInfo().getSuccess(), event.getToolCallInfo().getExecutionTime(),
                    event.getToolExecutionStartTime());

            // 保存工具调用到数据库，使用事件中的工具执行开始时间
            traceDomainService.recordToolCall(event.getTraceContext(), event.getToolCallInfo(),
                    event.getToolExecutionStartTime());

            logger.debug("工具调用已保存 - TraceId: {}, ToolName: {}, Success: {}, StartTime: {}",
                    event.getTraceContext().getTraceId(), event.getToolCallInfo().getToolName(),
                    event.getToolCallInfo().getSuccess(), event.getToolExecutionStartTime());

        } catch (Exception e) {
            logger.error("处理工具执行事件失败 - TraceId: {}", event.getTraceContext().getTraceId(), e);
        }
    }

    /** 处理执行完成事件 */
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

            // 完成追踪记录并保存到数据库
            traceDomainService.completeTrace(event.getTraceContext(), event.isSuccess(), event.getErrorPhase(),
                    event.getErrorMessage());

            logger.debug("追踪记录已完成并保存 - TraceId: {}, Success: {}", event.getTraceContext().getTraceId(),
                    event.isSuccess());

        } catch (Exception e) {
            logger.error("处理执行完成事件失败 - TraceId: {}",
                    event.getTraceContext() != null ? event.getTraceContext().getTraceId() : "UNKNOWN", e);
        }
    }
}