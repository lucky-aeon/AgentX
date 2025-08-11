package org.xhy.application.trace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Agent执行链路统计信息DTO */
public class AgentTraceStatisticsDTO {

    /** Agent ID */
    private String agentId;

    /** Agent名称 */
    private String agentName;

    /** 总执行次数 */
    private Integer totalExecutions;

    /** 成功执行次数 */
    private Integer successfulExecutions;

    /** 失败执行次数 */
    private Integer failedExecutions;

    /** 成功率 */
    private Double successRate;

    /** 总Token数 */
    private Integer totalTokens;

    /** 总输入Token数 */
    private Integer totalInputTokens;

    /** 总输出Token数 */
    private Integer totalOutputTokens;

    /** 工具调用总次数 */
    private Integer totalToolCalls;


    /** 会话总数 */
    private Integer totalSessions;

    /** 最后执行时间 */
    private LocalDateTime lastExecutionTime;

    /** 最后执行状态 */
    private Boolean lastExecutionSuccess;

    public AgentTraceStatisticsDTO() {
    }

    // Getter和Setter方法
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

    public Integer getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(Integer totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public Integer getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public void setSuccessfulExecutions(Integer successfulExecutions) {
        this.successfulExecutions = successfulExecutions;
    }

    public Integer getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(Integer failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
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

    public Integer getTotalToolCalls() {
        return totalToolCalls;
    }

    public void setTotalToolCalls(Integer totalToolCalls) {
        this.totalToolCalls = totalToolCalls;
    }


    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public LocalDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(LocalDateTime lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public Boolean getLastExecutionSuccess() {
        return lastExecutionSuccess;
    }

    public void setLastExecutionSuccess(Boolean lastExecutionSuccess) {
        this.lastExecutionSuccess = lastExecutionSuccess;
    }
}