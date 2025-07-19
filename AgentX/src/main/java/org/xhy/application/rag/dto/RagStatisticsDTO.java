package org.xhy.application.rag.dto;

/** RAG统计数据DTO
 * @author xhy
 * @date 2025-07-18 <br/>
 */
public class RagStatisticsDTO {

    /** 总RAG数量 */
    private Long totalRags;

    /** 待审核数量 */
    private Long pendingReview;

    /** 已发布数量 */
    private Long approved;

    /** 已拒绝数量 */
    private Long rejected;

    /** 已下架数量 */
    private Long removed;

    /** 总安装次数 */
    private Long totalInstalls;

    public Long getTotalRags() {
        return totalRags;
    }

    public void setTotalRags(Long totalRags) {
        this.totalRags = totalRags;
    }

    public Long getPendingReview() {
        return pendingReview;
    }

    public void setPendingReview(Long pendingReview) {
        this.pendingReview = pendingReview;
    }

    public Long getApproved() {
        return approved;
    }

    public void setApproved(Long approved) {
        this.approved = approved;
    }

    public Long getRejected() {
        return rejected;
    }

    public void setRejected(Long rejected) {
        this.rejected = rejected;
    }

    public Long getRemoved() {
        return removed;
    }

    public void setRemoved(Long removed) {
        this.removed = removed;
    }

    public Long getTotalInstalls() {
        return totalInstalls;
    }

    public void setTotalInstalls(Long totalInstalls) {
        this.totalInstalls = totalInstalls;
    }
}