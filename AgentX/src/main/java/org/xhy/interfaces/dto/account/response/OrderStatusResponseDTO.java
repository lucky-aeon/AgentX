package org.xhy.interfaces.dto.account.response;

import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.domain.order.constant.PaymentType;

import java.math.BigDecimal;

/** 订单状态响应DTO */
public class OrderStatusResponseDTO {

    /** 订单ID */
    private String orderId;

    /** 订单号 */
    private String orderNo;

    /** 订单状态 */
    private OrderStatus status;

    /** 支付平台 */
    private PaymentPlatform paymentPlatform;

    /** 支付类型 */
    private PaymentType paymentType;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单标题 */
    private String title;

    /** 支付URL（二维码内容或跳转链接） */
    private String paymentUrl;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

    /** 过期时间 */
    private String expiredAt;

    public OrderStatusResponseDTO() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentPlatform getPaymentPlatform() {
        return paymentPlatform;
    }

    public void setPaymentPlatform(PaymentPlatform paymentPlatform) {
        this.paymentPlatform = paymentPlatform;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
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

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(String expiredAt) {
        this.expiredAt = expiredAt;
    }

    @Override
    public String toString() {
        return "OrderStatusResponseDTO{" + "orderId='" + orderId + '\'' + ", orderNo='" + orderNo + '\'' + ", status='"
                + status + '\'' + ", paymentPlatform='" + paymentPlatform + '\'' + ", paymentType='" + paymentType
                + '\'' + ", amount=" + amount + ", title='" + title + '\'' + ", paymentUrl='" + paymentUrl + '\''
                + ", createdAt='" + createdAt + '\'' + ", updatedAt='" + updatedAt + '\'' + ", expiredAt='" + expiredAt
                + '\'' + '}';
    }
}