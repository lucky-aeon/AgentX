package org.xhy.application.trace.collector;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.trace.constant.ExecutionPhase;
import org.xhy.domain.trace.event.*;
import org.xhy.domain.trace.model.ModelCallInfo;
import org.xhy.domain.trace.model.ToolCallInfo;
import org.xhy.domain.trace.model.TraceContext;
import org.xhy.domain.trace.service.AgentExecutionTraceDomainService;

/** 追踪数据收集器 负责在关键执行节点收集追踪数据 */
@Component
public class TraceCollector {

    private static final Logger logger = LoggerFactory.getLogger(TraceCollector.class);
    private final AgentExecutionTraceDomainService traceDomainService;
    private final ApplicationEventPublisher eventPublisher;

    public TraceCollector(AgentExecutionTraceDomainService traceDomainService,
            ApplicationEventPublisher eventPublisher) {
        this.traceDomainService = traceDomainService;
        this.eventPublisher = eventPublisher;
    }

    /** 开始执行追踪
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param agentId Agent ID
     * @param userMessage 用户消息
     * @param messageType 消息类型
     * @return 追踪上下文 */
    public TraceContext startExecution(String userId, String sessionId, String agentId, String userMessage,
            String messageType) {
        try {
            // 创建追踪上下文，暂不记录用户消息
            TraceContext traceContext = traceDomainService.createTrace(userId, sessionId, agentId);

            // 将用户消息信息存储到追踪上下文中，等待模型调用时一起处理
            traceContext.setUserMessage(userMessage);
            traceContext.setUserMessageType(messageType);

            return traceContext;
        } catch (Exception e) {
            // 追踪失败不影响主流程，返回禁用的上下文
            return TraceContext.createDisabled();
        }
    }

    /** 记录模型调用
     * 
     * @param traceContext 追踪上下文
     * @param aiResponse AI响应内容
     * @param modelCallInfo 模型调用信息 */
    public void recordModelCall(TraceContext traceContext, String aiResponse, ModelCallInfo modelCallInfo) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        try {
            // 发布模型调用事件，监听器负责记录用户消息和AI响应
            eventPublisher.publishEvent(new ModelCalledEvent(this, traceContext, aiResponse, modelCallInfo));
        } catch (Exception e) {
            // 静默处理异常，不影响主流程
            // 可以记录日志进行监控
        }
    }

    /** 记录工具调用
     * 
     * @param traceContext 追踪上下文
     * @param toolCallInfo 工具调用信息 */
    public void recordToolCall(TraceContext traceContext, ToolCallInfo toolCallInfo) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        try {
            // 发布工具执行事件，监听器负责记录工具调用
            eventPublisher.publishEvent(new ToolExecutedEvent(this, traceContext, toolCallInfo));
        } catch (Exception e) {
            // 静默处理异常，不影响主流程
        }
    }

    /** 完成执行追踪
     * 
     * @param traceContext 追踪上下文
     * @param success 是否成功
     * @param errorPhase 错误阶段
     * @param errorMessage 错误信息 */
    public void completeExecution(TraceContext traceContext, boolean success, ExecutionPhase errorPhase,
            String errorMessage) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        try {
            // 发布执行完成事件，监听器负责完成追踪记录
            eventPublisher
                    .publishEvent(new ExecutionCompletedEvent(this, traceContext, success, errorPhase, errorMessage));
        } catch (Exception e) {
            // 静默处理异常，不影响主流程
        }
    }

    /** 记录执行成功
     * 
     * @param traceContext 追踪上下文 */
    public void recordSuccess(TraceContext traceContext) {
        completeExecution(traceContext, true, null, null);
    }

    /** 记录执行失败
     * 
     * @param traceContext 追踪上下文
     * @param errorPhase 错误阶段
     * @param errorMessage 错误信息 */
    public void recordFailure(TraceContext traceContext, ExecutionPhase errorPhase, String errorMessage) {
        completeExecution(traceContext, false, errorPhase, errorMessage);
    }

    /** 记录执行失败（使用Throwable）
     * 
     * @param traceContext 追踪上下文
     * @param errorPhase 错误阶段
     * @param throwable 异常信息 */
    public void recordFailure(TraceContext traceContext, ExecutionPhase errorPhase, Throwable throwable) {
        String errorMessage = throwable != null ? throwable.getMessage() : "未知错误";
        recordFailure(traceContext, errorPhase, errorMessage);
    }
}