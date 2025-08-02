package org.xhy.application.trace.dto;

/** 执行统计响应DTO */
public class ExecutionStatisticsResponse {

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

    public ExecutionStatisticsResponse() {
    }

    public ExecutionStatisticsResponse(Integer totalExecutions, Integer successfulExecutions, Integer failedExecutions,
            Double successRate, Long totalTokens) {
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = failedExecutions;
        this.successRate = successRate;
        this.totalTokens = totalTokens;
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

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
}