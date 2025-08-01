package org.xhy.domain.trace.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 追踪上下文
 * 用于在执行过程中传递追踪信息
 */
public class TraceContext {
    
    /** 追踪ID */
    private final String traceId;
    
    /** 用户ID */
    private final Long userId;
    
    /** 会话ID */
    private final String sessionId;
    
    /** Agent ID */
    private final String agentId;
    
    /** 执行开始时间 */
    private final LocalDateTime startTime;
    
    /** 序列号生成器 */
    private final AtomicInteger sequenceGenerator;
    
    /** 是否启用追踪 */
    private final boolean traceEnabled;

    public TraceContext(String traceId, Long userId, String sessionId, String agentId, boolean traceEnabled) {
        this.traceId = traceId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.agentId = agentId;
        this.startTime = LocalDateTime.now();
        this.sequenceGenerator = new AtomicInteger(0);
        this.traceEnabled = traceEnabled;
    }

    /**
     * 创建追踪上下文
     */
    public static TraceContext create(String traceId, Long userId, String sessionId, String agentId) {
        return new TraceContext(traceId, userId, sessionId, agentId, true);
    }

    /**
     * 创建禁用追踪的上下文
     */
    public static TraceContext createDisabled() {
        return new TraceContext(null, null, null, null, false);
    }

    /**
     * 生成下一个序列号
     */
    public int nextSequence() {
        return sequenceGenerator.incrementAndGet();
    }

    /**
     * 获取当前序列号
     */
    public int getCurrentSequence() {
        return sequenceGenerator.get();
    }

    /**
     * 检查是否启用追踪
     */
    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    // Getter方法
    public String getTraceId() {
        return traceId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAgentId() {
        return agentId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return "TraceContext{" +
                "traceId='" + traceId + '\'' +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", agentId=" + agentId +
                ", startTime=" + startTime +
                ", currentSequence=" + sequenceGenerator.get() +
                ", traceEnabled=" + traceEnabled +
                '}';
    }
}