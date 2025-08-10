package org.xhy.interfaces.dto.account.response;

import java.math.BigDecimal;

/** 支付响应DTO */
public class PaymentResponseDTO {

    /** 订单ID */
    private String orderId;

    /** 订单号 */
    private String orderNo;

    /** 支付URL（网页支付时为跳转链接，二维码支付时为二维码内容） */
    private String paymentUrl;

    /** 支付平台 */
    private String paymentMethod;

    /** 支付类型 */
    private String paymentType;

    /** 支付金额 */
    private BigDecimal amount;

    /** 订单标题 */
    private String title;

    /** 订单状态 */
    private String status;

    public PaymentResponseDTO() {
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

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PaymentResponseDTO{" + "orderId='" + orderId + '\'' + ", orderNo='" + orderNo + '\'' + ", paymentUrl='"
                + paymentUrl + '\'' + ", paymentMethod='" + paymentMethod + '\'' + ", paymentType='" + paymentType
                + '\'' + ", amount=" + amount + ", title='" + title + '\'' + ", status='" + status + '\'' + '}';
    }
}