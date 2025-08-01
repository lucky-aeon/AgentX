package org.xhy.application.trace.dto;

/**
 * 执行统计信息DTO
 */
public class ExecutionStatisticsDTO {
    
    /** 总执行次数 */
    private Integer totalExecutions;
    
    /** 成功执行次数 */
    private Integer successfulExecutions;
    
    /** 失败执行次数 */
    private Integer failedExecutions;
    
    /** 成功率 */
    private Double successRate;
    
    /** 总Token使用量 */
    private Long totalTokens;
    
    /** 平均执行时间(毫秒) */
    private Double averageExecutionTime;
    
    /** 总工具调用次数 */
    private Integer totalToolCalls;

    public ExecutionStatisticsDTO() {
    }

    public ExecutionStatisticsDTO(Integer totalExecutions, Integer successfulExecutions, Long totalTokens) {
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = totalExecutions - successfulExecutions;
        this.successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        this.totalTokens = totalTokens;
    }

    // Getter和Setter方法
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

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Double getAverageExecutionTime() {
        return averageExecutionTime;
    }

    public void setAverageExecutionTime(Double averageExecutionTime) {
        this.averageExecutionTime = averageExecutionTime;
    }

    public Integer getTotalToolCalls() {
        return totalToolCalls;
    }

    public void setTotalToolCalls(Integer totalToolCalls) {
        this.totalToolCalls = totalToolCalls;
    }
}