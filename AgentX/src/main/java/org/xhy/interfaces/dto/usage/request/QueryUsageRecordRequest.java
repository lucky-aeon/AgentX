package org.xhy.interfaces.dto.usage.request;

import org.xhy.interfaces.dto.Page;

import java.time.LocalDateTime;

/** 查询使用记录请求 */
public class QueryUsageRecordRequest extends Page {

    /** 用户ID */
    private String userId;

    /** 商品ID */
    private String productId;

    /** 请求ID */
    private String requestId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    public QueryUsageRecordRequest() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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
}