package org.xhy.application.trace.dto;

import org.xhy.interfaces.dto.Page;

import java.time.LocalDateTime;

/** Agent执行链路列表查询请求DTO */
public class AgentTraceListRequest extends Page {

    /** 关键词搜索（Agent名称模糊匹配） */
    private String keyword;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 执行状态筛选：true-有成功执行，false-仅失败，null-全部 */
    private Boolean hasSuccessfulExecution;

    public AgentTraceListRequest() {
    }

    // Getter和Setter方法
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getHasSuccessfulExecution() {
        return hasSuccessfulExecution;
    }

    public void setHasSuccessfulExecution(Boolean hasSuccessfulExecution) {
        this.hasSuccessfulExecution = hasSuccessfulExecution;
    }
}