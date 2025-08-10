package org.xhy.infrastructure.payment.model;

import java.util.Map;

/** 支付结果封装类 */
public class PaymentResult {

    /** 是否成功 */
    private boolean success;

    /** 支付URL（需要跳转支付时使用） */
    private String paymentUrl;

    /** 第三方订单ID */
    private String providerOrderId;

    /** 第三方支付ID */
    private String providerPaymentId;

    /** 支付方式 */
    private String paymentMethod;

    /** 支付类型 */
    private String paymentType;

    /** 支付状态 */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 错误代码 */
    private String errorCode;

    /** 扩展数据 */
    private Map<String, Object> extraData;

    /** 原始响应数据 */
    private Map<String, Object> rawResponse;

    public PaymentResult() {
    }

    public PaymentResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    public Map<String, Object> getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(Map<String, Object> rawResponse) {
        this.rawResponse = rawResponse;
    }

    /** 创建成功结果 */
    public static PaymentResult success() {
        return new PaymentResult(true);
    }

    /** 创建成功结果（带支付URL） */
    public static PaymentResult success(String paymentUrl) {
        PaymentResult result = success();
        result.setPaymentUrl(paymentUrl);
        return result;
    }

    /** 创建成功结果（带第三方订单ID） */
    public static PaymentResult success(String providerOrderId, String paymentUrl) {
        PaymentResult result = success(paymentUrl);
        result.setProviderOrderId(providerOrderId);
        return result;
    }

    /** 创建成功结果（带支付方式和类型） */
    public static PaymentResult success(String paymentUrl, String paymentMethod, String paymentType) {
        PaymentResult result = success(paymentUrl);
        result.setPaymentMethod(paymentMethod);
        result.setPaymentType(paymentType);
        return result;
    }

    /** 创建成功结果（完整参数） */
    public static PaymentResult success(String providerOrderId, String paymentUrl, String paymentMethod,
            String paymentType) {
        PaymentResult result = success(providerOrderId, paymentUrl);
        result.setPaymentMethod(paymentMethod);
        result.setPaymentType(paymentType);
        return result;
    }

    /** 创建失败结果 */
    public static PaymentResult failure(String errorMessage) {
        PaymentResult result = new PaymentResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /** 创建失败结果（带错误代码） */
    public static PaymentResult failure(String errorCode, String errorMessage) {
        PaymentResult result = failure(errorMessage);
        result.setErrorCode(errorCode);
        return result;
    }

    /** 检查是否需要跳转支付 */
    public boolean needRedirect() {
        return success && paymentUrl != null && !paymentUrl.trim().isEmpty();
    }

    /** 检查是否为失败结果 */
    public boolean isFailure() {
        return !success;
    }

    @Override
    public String toString() {
        return "PaymentResult{" + "success=" + success + ", paymentUrl='" + paymentUrl + '\'' + ", providerOrderId='"
                + providerOrderId + '\'' + ", providerPaymentId='" + providerPaymentId + '\'' + ", errorMessage='"
                + errorMessage + '\'' + ", errorCode='" + errorCode + '\'' + '}';
    }
}