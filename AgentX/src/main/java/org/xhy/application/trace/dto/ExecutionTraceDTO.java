package org.xhy.application.trace.dto;

import java.util.List;

/**
 * 完整的执行链路DTO
 * 包含汇总信息和详细步骤信息
 */
public class ExecutionTraceDTO {
    
    /** 执行汇总信息 */
    private AgentExecutionSummaryDTO summary;
    
    /** 执行详细步骤列表 */
    private List<AgentExecutionDetailDTO> details;
    
    /** 用户消息步骤 */
    private List<AgentExecutionDetailDTO> userMessages;
    
    /** AI响应步骤 */
    private List<AgentExecutionDetailDTO> aiResponses;
    
    /** 工具调用步骤 */
    private List<AgentExecutionDetailDTO> toolCalls;
    
    /** 降级调用步骤 */
    private List<AgentExecutionDetailDTO> fallbackCalls;
    
    /** 失败的步骤 */
    private List<AgentExecutionDetailDTO> failedSteps;

    public ExecutionTraceDTO() {
    }

    public ExecutionTraceDTO(AgentExecutionSummaryDTO summary, List<AgentExecutionDetailDTO> details) {
        this.summary = summary;
        this.details = details;
    }

    // Getter和Setter方法
    public AgentExecutionSummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(AgentExecutionSummaryDTO summary) {
        this.summary = summary;
    }

    public List<AgentExecutionDetailDTO> getDetails() {
        return details;
    }

    public void setDetails(List<AgentExecutionDetailDTO> details) {
        this.details = details;
    }

    public List<AgentExecutionDetailDTO> getUserMessages() {
        return userMessages;
    }

    public void setUserMessages(List<AgentExecutionDetailDTO> userMessages) {
        this.userMessages = userMessages;
    }

    public List<AgentExecutionDetailDTO> getAiResponses() {
        return aiResponses;
    }

    public void setAiResponses(List<AgentExecutionDetailDTO> aiResponses) {
        this.aiResponses = aiResponses;
    }

    public List<AgentExecutionDetailDTO> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<AgentExecutionDetailDTO> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<AgentExecutionDetailDTO> getFallbackCalls() {
        return fallbackCalls;
    }

    public void setFallbackCalls(List<AgentExecutionDetailDTO> fallbackCalls) {
        this.fallbackCalls = fallbackCalls;
    }

    public List<AgentExecutionDetailDTO> getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(List<AgentExecutionDetailDTO> failedSteps) {
        this.failedSteps = failedSteps;
    }
}