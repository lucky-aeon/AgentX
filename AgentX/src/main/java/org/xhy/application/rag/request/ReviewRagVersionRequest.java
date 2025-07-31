package org.xhy.application.rag.request;

import jakarta.validation.constraints.NotNull;

/** 审核RAG版本请求
 * @author xhy
 * @date 2025-07-16 <br/>
 */
public class ReviewRagVersionRequest {

    /** 审核状态：2:已发布, 3:拒绝 */
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    /** 拒绝原因（拒绝时必填） */
    private String rejectReason;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}