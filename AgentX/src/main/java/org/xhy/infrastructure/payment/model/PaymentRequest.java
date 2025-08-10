package org.xhy.infrastructure.payment.model;

import java.math.BigDecimal;
import java.util.Map;

/** 支付请求封装类 */
public class PaymentRequest {

    /** 订单ID */
    private String orderId;

    /** 支付ID */
    private String paymentId;

    /** 订单号 */
    private String orderNo;

    /** 订单标题 */
    private String title;

    /** 订单描述 */
    private String description;

    /** 支付金额 */
    private BigDecimal amount;

    /** 货币代码 */
    private String currency;

    /** 用户ID */
    private String userId;

    /** 支付成功回调URL */
    private String successUrl;

    /** 支付取消回调URL */
    private String cancelUrl;

    /** 异步通知URL */
    private String notifyUrl;

    /** 客户端IP */
    private String clientIp;

    /** 用户代理 */
    private String userAgent;

    /** 支付类型 */
    private String paymentType;

    /** 扩展参数 */
    private Map<String, String> extraParams;

    public PaymentRequest() {
        this.currency = "CNY";
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, String> getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(Map<String, String> extraParams) {
        this.extraParams = extraParams;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    /** 获取扩展参数值 */
    public String getExtraParam(String key) {
        return extraParams != null ? extraParams.get(key) : null;
    }

    /** 设置扩展参数值 */
    public void setExtraParam(String key, String value) {
        if (extraParams == null) {
            extraParams = new java.util.HashMap<>();
        }
        extraParams.put(key, value);
    }

    /** 验证请求参数 */
    public void validate() {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("支付ID不能为空");
        }
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("订单标题不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付金额必须大于0");
        }
        if (currency == null || currency.trim().isEmpty()) {
            this.currency = "CNY";
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (notifyUrl == null || notifyUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("异步通知URL不能为空");
        }
    }

    @Override
    public String toString() {
        return "PaymentRequest{" + "orderId='" + orderId + '\'' + ", paymentId='" + paymentId + '\'' + ", orderNo='"
                + orderNo + '\'' + ", title='" + title + '\'' + ", amount=" + amount + ", currency='" + currency + '\''
                + ", userId='" + userId + '\'' + '}';
    }
}