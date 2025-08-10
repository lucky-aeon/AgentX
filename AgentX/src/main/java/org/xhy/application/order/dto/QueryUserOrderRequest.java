package org.xhy.application.order.dto;

import org.xhy.interfaces.dto.Page;

/** 用户订单查询请求 */
public class QueryUserOrderRequest extends Page {

    /** 订单类型（可选） */
    private String orderType;

    /** 订单状态（可选） */
    private Integer status;

    public QueryUserOrderRequest() {
        super();
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
}