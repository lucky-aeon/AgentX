package org.xhy.application.trace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Agent执行链路汇总DTO */
public class AgentExecutionSummaryDTO {

    /** 追踪ID */
    private String traceId;

    /** 用户ID */
    private String userId;

    /** 会话ID */
    private String sessionId;

    /** Agent ID */
    private String agentId;

    /** Agent名称 */
    private String agentName;

    /** 执行开始时间 */
    private LocalDateTime executionStartTime;

    /** 执行结束时间 */
    private LocalDateTime executionEndTime;

    /** 总执行时间(毫秒) */
    private Integer totalExecutionTime;

    /** 总输入Token数 */
    private Integer totalInputTokens;

    /** 总输出Token数 */
    private Integer totalOutputTokens;

    /** 总Token数 */
    private Integer totalTokens;

    /** 工具调用总次数 */
    private Integer toolCallCount;

    /** 工具执行总耗时(毫秒) */
    private Integer totalToolExecutionTime;


    /** 执行是否成功 */
    private Boolean executionSuccess;

    /** 错误发生阶段 */
    private String errorPhase;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdTime;

    public AgentExecutionSummaryDTO() {
    }

    // Getter和Setter方法
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public LocalDateTime getExecutionStartTime() {
        return executionStartTime;
    }

    public void setExecutionStartTime(LocalDateTime executionStartTime) {
        this.executionStartTime = executionStartTime;
    }

    public LocalDateTime getExecutionEndTime() {
        return executionEndTime;
    }

    public void setExecutionEndTime(LocalDateTime executionEndTime) {
        this.executionEndTime = executionEndTime;
    }

    public Integer getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public void setTotalExecutionTime(Integer totalExecutionTime) {
        this.totalExecutionTime = totalExecutionTime;
    }

    public Integer getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(Integer totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public Integer getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(Integer totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getToolCallCount() {
        return toolCallCount;
    }

    public void setToolCallCount(Integer toolCallCount) {
        this.toolCallCount = toolCallCount;
    }

    public Integer getTotalToolExecutionTime() {
        return totalToolExecutionTime;
    }

    public void setTotalToolExecutionTime(Integer totalToolExecutionTime) {
        this.totalToolExecutionTime = totalToolExecutionTime;
    }


    public Boolean getExecutionSuccess() {
        return executionSuccess;
    }

    public void setExecutionSuccess(Boolean executionSuccess) {
        this.executionSuccess = executionSuccess;
    }

    public String getErrorPhase() {
        return errorPhase;
    }

    public void setErrorPhase(String errorPhase) {
        this.errorPhase = errorPhase;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}