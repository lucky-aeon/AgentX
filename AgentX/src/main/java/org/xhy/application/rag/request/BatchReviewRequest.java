package org.xhy.application.rag.request;

import java.util.List;

/** 批量审核请求
 * @author xhy
 * @date 2025-07-18 <br/>
 */
public class BatchReviewRequest {

    /** 版本ID列表 */
    private List<String> versionIds;

    /** 审核状态：2:已发布, 3:拒绝, 4:已下架 */
    private Integer status;

    /** 拒绝原因（拒绝时必填） */
    private String rejectReason;

    public List<String> getVersionIds() {
        return versionIds;
    }

    public void setVersionIds(List<String> versionIds) {
        this.versionIds = versionIds;
    }

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