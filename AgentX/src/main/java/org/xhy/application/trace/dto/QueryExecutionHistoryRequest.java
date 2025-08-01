package org.xhy.application.trace.dto;

import org.xhy.interfaces.dto.Page;

import java.time.LocalDateTime;

/**
 * 查询执行历史请求DTO
 */
public class QueryExecutionHistoryRequest extends Page {
    
    /** 会话ID */
    private String sessionId;
    
    /** Agent ID */
    private String agentId;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 执行状态：true-成功，false-失败，null-全部 */
    private Boolean executionSuccess;
    
    /** 关键词搜索 */
    private String keyword;

    public QueryExecutionHistoryRequest() {
    }

    // Getter和Setter方法
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

    public Boolean getExecutionSuccess() {
        return executionSuccess;
    }

    public void setExecutionSuccess(Boolean executionSuccess) {
        this.executionSuccess = executionSuccess;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}