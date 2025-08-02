package org.xhy.application.trace.dto;

import java.util.List;

/** 追踪详情响应DTO 包含执行汇总和详细步骤信息 */
public class TraceDetailResponse {

    /** 执行汇总信息 */
    private AgentExecutionSummaryDTO summary;

    /** 执行详细步骤列表 */
    private List<AgentExecutionDetailDTO> details;

    public TraceDetailResponse() {
    }

    public TraceDetailResponse(AgentExecutionSummaryDTO summary, List<AgentExecutionDetailDTO> details) {
        this.summary = summary;
        this.details = details;
    }

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
}