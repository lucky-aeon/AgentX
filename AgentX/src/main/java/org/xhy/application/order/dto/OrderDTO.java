package org.xhy.application.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** 订单DTO */
public class OrderDTO {

    /** 订单ID */
    private String id;

    /** 用户ID */
    private String userId;

    /** 用户昵称 */
    private String userNickname;

    /** 订单号 */
    private String orderNo;

    /** 订单类型 */
    private String orderType;

    /** 订单标题 */
    private String title;

    /** 订单描述 */
    private String description;

    /** 订单金额 */
    private BigDecimal amount;

    /** 货币代码 */
    private String currency;

    /** 订单状态 */
    private Integer status;

    /** 订单状态名称 */
    private String statusName;

    /** 订单过期时间 */
    private LocalDateTime expiredAt;

    /** 支付完成时间 */
    private LocalDateTime paidAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    /** 退款时间 */
    private LocalDateTime refundedAt;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 支付平台 */
    private String paymentPlatform;

    /** 支付平台名称 */
    private String paymentPlatformName;

    /** 支付类型 */
    private String paymentType;

    /** 支付类型名称 */
    private String paymentTypeName;

    /** 第三方支付平台的订单ID */
    private String providerOrderId;

    /** 订单扩展信息 */
    private Map<String, Object> metadata;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public OrderDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPaymentPlatform() {
        return paymentPlatform;
    }

    public void setPaymentPlatform(String paymentPlatform) {
        this.paymentPlatform = paymentPlatform;
    }

    public String getPaymentPlatformName() {
        return paymentPlatformName;
    }

    public void setPaymentPlatformName(String paymentPlatformName) {
        this.paymentPlatformName = paymentPlatformName;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentTypeName() {
        return paymentTypeName;
    }

    public void setPaymentTypeName(String paymentTypeName) {
        this.paymentTypeName = paymentTypeName;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }
}