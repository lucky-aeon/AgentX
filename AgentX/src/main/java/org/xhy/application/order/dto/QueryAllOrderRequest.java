package org.xhy.application.order.dto;

import org.xhy.interfaces.dto.Page;

/** 管理员订单查询请求 */
public class QueryAllOrderRequest extends Page {

    /** 用户ID（可选） */
    private String userId;

    /** 订单类型（可选） */
    private String orderType;

    /** 订单状态（可选） */
    private Integer status;

    /** 关键词搜索（可选） - 搜索订单号、标题、描述 */
    private String keyword;

    public QueryAllOrderRequest() {
        super();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}