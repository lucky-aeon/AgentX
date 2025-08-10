package org.xhy.infrastructure.payment.model;

import java.math.BigDecimal;
import java.util.Map;

/** 支付回调数据封装类 */
public class PaymentCallback {

    /** 是否验签成功 */
    private boolean signatureValid;

    /** 支付是否成功 */
    private boolean paymentSuccess;

    /** 订单号 */
    private String orderNo;

    /** 第三方订单ID */
    private String providerOrderId;

    /** 第三方支付ID */
    private String providerPaymentId;

    /** 支付金额 */
    private BigDecimal amount;

    /** 货币代码 */
    private String currency;

    /** 支付状态 */
    private String paymentStatus;

    /** 支付方式 */
    private String paymentMethod;

    /** 支付时间（第三方时间） */
    private String paymentTime;

    /** 买家信息 */
    private String buyerInfo;

    /** 错误信息 */
    private String errorMessage;

    /** 错误代码 */
    private String errorCode;

    /** 原始回调数据 */
    private Map<String, Object> rawData;

    /** 扩展数据 */
    private Map<String, Object> extraData;

    public PaymentCallback() {
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public boolean isPaymentSuccess() {
        return paymentSuccess;
    }

    public void setPaymentSuccess(boolean paymentSuccess) {
        this.paymentSuccess = paymentSuccess;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(String paymentTime) {
        this.paymentTime = paymentTime;
    }

    public String getBuyerInfo() {
        return buyerInfo;
    }

    public void setBuyerInfo(String buyerInfo) {
        this.buyerInfo = buyerInfo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    /** 检查是否为有效的成功回调 */
    public boolean isValid() {
        return signatureValid && paymentSuccess;
    }

    /** 检查是否为失败回调 */
    public boolean isFailure() {
        return signatureValid && !paymentSuccess;
    }

    /** 检查是否为无效回调（验签失败） */
    public boolean isInvalid() {
        return !signatureValid;
    }

    /** 获取扩展数据值 */
    public Object getExtraData(String key) {
        return extraData != null ? extraData.get(key) : null;
    }

    /** 设置扩展数据值 */
    public void setExtraData(String key, Object value) {
        if (extraData == null) {
            extraData = new java.util.HashMap<>();
        }
        extraData.put(key, value);
    }

    /** 获取原始数据值 */
    public Object getRawData(String key) {
        return rawData != null ? rawData.get(key) : null;
    }

    /** 设置原始数据值 */
    public void setRawData(String key, Object value) {
        if (rawData == null) {
            rawData = new java.util.HashMap<>();
        }
        rawData.put(key, value);
    }

    @Override
    public String toString() {
        return "PaymentCallback{" + "signatureValid=" + signatureValid + ", paymentSuccess=" + paymentSuccess
                + ", orderNo='" + orderNo + '\'' + ", providerOrderId='" + providerOrderId + '\''
                + ", providerPaymentId='" + providerPaymentId + '\'' + ", amount=" + amount + ", paymentStatus='"
                + paymentStatus + '\'' + ", paymentMethod='" + paymentMethod + '\'' + '}';
    }
}