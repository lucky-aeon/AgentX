package org.xhy.domain.order.event;

import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.model.OrderEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 购买成功事件 统一的订单支付成功事件，各业务模块根据订单类型自主判断是否处理 */
public class PurchaseSuccessEvent {

    /** 订单ID */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 订单号 */
    private String orderNo;

    /** 订单类型 - 关键字段，用于监听器判断是否需要处理 */
    private OrderType orderType;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单标题 */
    private String title;

    /** 订单描述 */
    private String description;

    /** 完整的订单实体信息 */
    private OrderEntity orderEntity;

    /** 事件发生时间 */
    private LocalDateTime occurredAt;

    public PurchaseSuccessEvent() {
        this.occurredAt = LocalDateTime.now();
    }

    public PurchaseSuccessEvent(OrderEntity orderEntity) {
        this();
        this.orderId = orderEntity.getId();
        this.userId = orderEntity.getUserId();
        this.orderNo = orderEntity.getOrderNo();
        this.orderType = orderEntity.getOrderType();
        this.amount = orderEntity.getAmount();
        this.title = orderEntity.getTitle();
        this.description = orderEntity.getDescription();
        this.orderEntity = orderEntity;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OrderEntity getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(OrderEntity orderEntity) {
        this.orderEntity = orderEntity;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        return "PurchaseSuccessEvent{" + "orderId='" + orderId + '\'' + ", userId='" + userId + '\'' + ", orderNo='"
                + orderNo + '\'' + ", orderType=" + orderType + ", amount=" + amount + ", title='" + title + '\''
                + ", occurredAt=" + occurredAt + '}';
    }
}