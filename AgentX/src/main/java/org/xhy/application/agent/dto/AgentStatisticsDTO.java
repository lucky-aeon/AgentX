package org.xhy.application.agent.dto;

/** Agent统计信息DTO */
public class AgentStatisticsDTO {

    /** 总Agent数量 */
    private long totalAgents;

    /** 启用的Agent数量 */
    private long enabledAgents;

    /** 禁用的Agent数量 */
    private long disabledAgents;

    /** 待审核版本数量 */
    private long pendingVersions;

    public long getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(long totalAgents) {
        this.totalAgents = totalAgents;
    }

    public long getEnabledAgents() {
        return enabledAgents;
    }

    public void setEnabledAgents(long enabledAgents) {
        this.enabledAgents = enabledAgents;
    }

    public long getDisabledAgents() {
        return disabledAgents;
    }

    public void setDisabledAgents(long disabledAgents) {
        this.disabledAgents = disabledAgents;
    }

    public long getPendingVersions() {
        return pendingVersions;
    }

    public void setPendingVersions(long pendingVersions) {
        this.pendingVersions = pendingVersions;
    }
}