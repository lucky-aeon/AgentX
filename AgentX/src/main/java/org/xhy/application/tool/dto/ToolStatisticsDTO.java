package org.xhy.application.tool.dto;

/** 工具统计信息DTO */
public class ToolStatisticsDTO {

    /** 总工具数量 */
    private long totalTools;

    /** 待审核工具数量 */
    private long pendingReviewTools;

    /** 人工审核工具数量 */
    private long manualReviewTools;

    /** 已通过工具数量 */
    private long approvedTools;

    /** 审核失败工具数量 */
    private long failedTools;

    /** 官方工具数量 */
    private long officialTools;

    public long getTotalTools() {
        return totalTools;
    }

    public void setTotalTools(long totalTools) {
        this.totalTools = totalTools;
    }

    public long getPendingReviewTools() {
        return pendingReviewTools;
    }

    public void setPendingReviewTools(long pendingReviewTools) {
        this.pendingReviewTools = pendingReviewTools;
    }

    public long getManualReviewTools() {
        return manualReviewTools;
    }

    public void setManualReviewTools(long manualReviewTools) {
        this.manualReviewTools = manualReviewTools;
    }

    public long getApprovedTools() {
        return approvedTools;
    }

    public void setApprovedTools(long approvedTools) {
        this.approvedTools = approvedTools;
    }

    public long getFailedTools() {
        return failedTools;
    }

    public void setFailedTools(long failedTools) {
        this.failedTools = failedTools;
    }

    public long getOfficialTools() {
        return officialTools;
    }

    public void setOfficialTools(long officialTools) {
        this.officialTools = officialTools;
    }
}