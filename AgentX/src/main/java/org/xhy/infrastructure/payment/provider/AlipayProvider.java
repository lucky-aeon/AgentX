package org.xhy.infrastructure.payment.provider;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.infrastructure.payment.constant.AlipayPaymentType;
import org.xhy.infrastructure.payment.model.PaymentCallback;
import org.xhy.infrastructure.payment.model.PaymentRequest;
import org.xhy.infrastructure.payment.model.PaymentResult;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** 支付宝支付提供商 */
@Component
public class AlipayProvider extends PaymentProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlipayProvider.class);

    @Override
    protected boolean supportsRefund() {
        return true;
    }

    @Override
    protected boolean supportsCancellation() {
        return false;
    }

    private void initializeConfig() throws Exception {
        if (!initialized && isConfigured()) {
            synchronized (this) {
                if (!initialized) {
                    Config config = new Config();
                    config.protocol = "https";
                    config.gatewayHost = this.gatewayHost;
                    config.signType = this.signType;
                    config.appId = this.appId;
                    config.merchantPrivateKey = this.privateKey;
                    config.alipayPublicKey = this.alipayPublicKey;
                    config.notifyUrl = "";

                    Factory.setOptions(config);
                    initialized = true;
                    logger.info("支付宝 EasySDK 初始化成功");
                }
            }
        } else if (!isConfigured()) {
            throw new IllegalStateException("支付宝配置不完整，请检查 app-id, private-key, alipay-public-key 配置");
        }
    }

    @Value("${payment.alipay.app-id:}")
    private String appId;

    @Value("${payment.alipay.private-key:}")
    private String privateKey;

    @Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${payment.alipay.gateway-host:openapi-sandbox.dl.alipaydev.com}")
    private String gatewayHost;

    @Value("${payment.alipay.sign-type:RSA2}")
    private String signType;

    private volatile boolean initialized = false;

    @Override
    public PaymentPlatform getPaymentPlatform() {
        return PaymentPlatform.ALIPAY;
    }

    @Override
    public String getProviderCode() {
        return "alipay";
    }

    @Override
    public String getProviderName() {
        return "支付宝";
    }

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        try {
            initializeConfig();
            request.validate();

            // 获取支付类型，默认为WEB支付
            String paymentType = request.getPaymentType();
            if (paymentType == null || !AlipayPaymentType.isValid(paymentType)) {
                paymentType = AlipayPaymentType.getDefault();
            }

            logger.info("创建支付宝支付请求: orderId={}, amount={}, paymentType={}", request.getOrderId(), request.getAmount(),
                    paymentType);

            PaymentResult result;
            switch (paymentType) {
                case AlipayPaymentType.WEB :
                    result = createWebPayment(request);
                    break;
                case AlipayPaymentType.QR_CODE :
                    result = createQrCodePayment(request);
                    break;
                default :
                    result = createWebPayment(request);
                    break;
            }

            // 设置支付方式和类型
            if (result.isSuccess()) {
                result.setPaymentMethod("ALIPAY");
                result.setPaymentType(paymentType);
            }

            return result;

        } catch (Exception e) {
            logger.error("支付宝支付创建异常: orderId={}", request.getOrderId(), e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    /** 创建网页支付 */
    private PaymentResult createWebPayment(PaymentRequest request) throws Exception {
        AlipayTradePagePayResponse response = Factory.Payment.Page().pay(request.getTitle(), request.getOrderNo(),
                formatAmount(request.getAmount().toString()), request.getSuccessUrl());

        if (ResponseChecker.success(response)) {
            PaymentResult result = PaymentResult.success();
            result.setPaymentUrl(response.body);
            result.setProviderOrderId(request.getOrderNo());
            logger.info("支付宝WEB支付创建成功: orderId={}", request.getOrderId());
            return result;
        } else {
            logger.error("支付宝WEB支付创建失败: orderId={}", request.getOrderId());
            return PaymentResult.failure("WEB_PAYMENT_CREATE_FAILED", "WEB支付创建失败");
        }
    }

    /** 创建二维码支付 */
    private PaymentResult createQrCodePayment(PaymentRequest request) throws Exception {
        AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace()
                .asyncNotify("https://7dc9c0c9.r8.vip.cpolar.cn/api/payments/callback/alipay")
                .preCreate(request.getTitle(), request.getOrderNo(), formatAmount(request.getAmount().toString()));

        if (ResponseChecker.success(response)) {
            PaymentResult result = PaymentResult.success();
            result.setPaymentUrl(response.qrCode); // 二维码内容字符串
            result.setProviderOrderId(request.getOrderNo());
            logger.info("支付宝二维码支付创建成功: orderId={}, qrCode={}", request.getOrderId(), response.qrCode);
            return result;
        } else {
            logger.error("支付宝二维码支付创建失败: orderId={}", request.getOrderId());
            return PaymentResult.failure("QR_CODE_PAYMENT_CREATE_FAILED", "二维码支付创建失败");
        }
    }

    @Override
    public PaymentResult queryPayment(String providerOrderId) {
        try {
            initializeConfig();

            logger.info("查询支付宝支付状态: providerOrderId={}", providerOrderId);

            AlipayTradeQueryResponse response = Factory.Payment.Common().query(providerOrderId);

            if (ResponseChecker.success(response)) {
                PaymentResult result = new PaymentResult();

                // 设置基本信息
                result.setProviderOrderId(response.outTradeNo);
                result.setProviderPaymentId(response.tradeNo);
                result.setPaymentMethod("ALIPAY");
                result.setPaymentType(AlipayPaymentType.QR_CODE); // 默认二维码支付，可以根据实际情况调整

                // 设置支付平台原始状态（重要！）
                result.setStatus(response.tradeStatus);

                // 检查支付状态
                if ("TRADE_SUCCESS".equals(response.tradeStatus) || "TRADE_FINISHED".equals(response.tradeStatus)) {
                    result.setSuccess(true);
                    logger.info("支付宝支付成功: providerOrderId={}, tradeNo={}, status={}", providerOrderId, response.tradeNo,
                            response.tradeStatus);
                } else {
                    result.setSuccess(false);
                    result.setErrorMessage("支付未成功, 状态: " + response.tradeStatus);
                    logger.info("支付宝支付未成功: providerOrderId={}, status={}", providerOrderId, response.tradeStatus);
                }

                // 设置原始响应数据
                Map<String, Object> rawResponse = new HashMap<>();
                rawResponse.put("outTradeNo", response.outTradeNo);
                rawResponse.put("tradeNo", response.tradeNo);
                rawResponse.put("tradeStatus", response.tradeStatus);
                rawResponse.put("totalAmount", response.totalAmount);
                rawResponse.put("receiptAmount", response.receiptAmount);
                rawResponse.put("buyerPayAmount", response.buyerPayAmount);
                rawResponse.put("pointAmount", response.pointAmount);
                rawResponse.put("invoiceAmount", response.invoiceAmount);
                rawResponse.put("sendPayDate", response.sendPayDate);
                result.setRawResponse(rawResponse);

                return result;
            } else {
                logger.error("支付宝支付查询失败: providerOrderId={}, msg={}, subMsg={}", providerOrderId, response.msg,
                        response.subMsg);
                return PaymentResult.failure("PAYMENT_QUERY_FAILED", "支付查询失败: " + response.subMsg);
            }

        } catch (Exception e) {
            logger.error("支付宝支付查询异常: providerOrderId={}", providerOrderId, e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    @Override
    public PaymentCallback handleCallback(HttpServletRequest request) {
        try {
            logger.info("处理支付宝支付回调");

            // 提取支付宝form参数数据
            Map<String, Object> callbackData = extractAlipayCallbackData(request);

            // 调用原有的处理逻辑
            return handleCallbackData(callbackData);

        } catch (Exception e) {
            logger.error("支付宝回调处理异常", e);
            PaymentCallback callback = new PaymentCallback();
            callback.setSignatureValid(false);
            callback.setPaymentSuccess(false);
            callback.setErrorMessage("回调处理异常: " + e.getMessage());
            return callback;
        }
    }

    /** 提取支付宝回调数据（form参数格式）
     * 
     * @param request HTTP请求对象
     * @return 回调数据Map */
    private Map<String, Object> extractAlipayCallbackData(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();

        // 提取所有请求参数（支持form-data和query参数）
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                // 如果只有一个值，直接存储，否则存储数组
                if (values.length == 1) {
                    data.put(key, values[0]);
                } else {
                    data.put(key, values);
                }
            }
        });

        return data;
    }

    /** 处理支付宝回调数据
     * 
     * @param callbackData 回调数据
     * @return 支付回调对象 */
    private PaymentCallback handleCallbackData(Map<String, Object> callbackData) {
        PaymentCallback callback = new PaymentCallback();
        callback.setRawData(callbackData);

        try {
            logger.info("处理支付宝支付回调: data={}", callbackData);

            // 验证签名
            boolean isValid = verifyAlipayCallback(callbackData);
            callback.setSignatureValid(isValid);

            if (!isValid) {
                logger.warn("支付宝回调验签失败");
                return callback;
            }

            // 解析回调数据
            String tradeStatus = (String) callbackData.get("trade_status");
            String outTradeNo = (String) callbackData.get("out_trade_no");
            String tradeNo = (String) callbackData.get("trade_no");
            String totalAmount = (String) callbackData.get("total_amount");
            String gmtPayment = (String) callbackData.get("gmt_payment");
            String buyerLogonId = (String) callbackData.get("buyer_logon_id");

            callback.setOrderNo(outTradeNo);
            callback.setProviderOrderId(outTradeNo);
            callback.setProviderPaymentId(tradeNo);
            callback.setPaymentTime(gmtPayment);
            callback.setBuyerInfo(buyerLogonId);
            callback.setCurrency("CNY");

            if (StringUtils.hasText(totalAmount)) {
                callback.setAmount(new BigDecimal(totalAmount));
            }

            // 判断支付状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                callback.setPaymentSuccess(true);
                callback.setPaymentStatus("SUCCESS");
                logger.info("支付宝支付成功回调: outTradeNo={}, tradeNo={}, amount={}", outTradeNo, tradeNo, totalAmount);
            } else {
                callback.setPaymentSuccess(false);
                callback.setPaymentStatus(tradeStatus);
                logger.info("支付宝支付未成功回调: outTradeNo={}, status={}", outTradeNo, tradeStatus);
            }

        } catch (Exception e) {
            logger.error("支付宝回调处理异常", e);
            callback.setSignatureValid(false);
            callback.setPaymentSuccess(false);
            callback.setErrorMessage("回调处理异常: " + e.getMessage());
        }

        return callback;
    }

    @Override
    public PaymentResult cancelPayment(String providerOrderId) {
        logger.warn("支付宝不支持取消支付操作: providerOrderId={}", providerOrderId);
        return PaymentResult.failure("NOT_SUPPORTED", "支付宝不支持取消支付操作");
    }

    @Override
    public PaymentResult refundPayment(String providerOrderId, String refundAmount, String refundReason) {
        try {
            initializeConfig();

            logger.info("申请支付宝退款: providerOrderId={}, amount={}, reason={}", providerOrderId, refundAmount,
                    refundReason);

            // 生成退款请求号
            String refundNo = "RF" + System.currentTimeMillis();

            AlipayTradeRefundResponse response = Factory.Payment.Common().refund(providerOrderId,
                    formatAmount(refundAmount));

            if (ResponseChecker.success(response)) {
                PaymentResult result = PaymentResult.success();
                result.setProviderOrderId(response.outTradeNo);
                result.setProviderPaymentId(response.tradeNo);

                // 设置退款信息
                Map<String, Object> extraData = new HashMap<>();
                extraData.put("refundAmount", response.refundFee);
                extraData.put("refundDate", response.gmtRefundPay);
                extraData.put("refundReason", refundReason);
                result.setExtraData(extraData);

                logger.info("支付宝退款成功: providerOrderId={}, refundAmount={}, tradeNo={}", providerOrderId,
                        response.refundFee, response.tradeNo);
                return result;
            } else {
                logger.error("支付宝退款失败: providerOrderId={}", providerOrderId);
                return PaymentResult.failure("REFUND_FAILED", "退款失败");
            }

        } catch (Exception e) {
            logger.error("支付宝退款异常: providerOrderId={}", providerOrderId, e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    /** 验证支付宝回调签名
     * 
     * @param callbackData 回调数据
     * @return 是否验证通过 */
    private boolean verifyAlipayCallback(Map<String, Object> callbackData) {
        try {
            initializeConfig();

            // 将Map转换为适合的格式
            Map<String, String> params = new HashMap<>();
            for (Map.Entry<String, Object> entry : callbackData.entrySet()) {
                if (entry.getValue() != null) {
                    params.put(entry.getKey(), entry.getValue().toString());
                }
            }

            // 使用EasySDK验证回调
            return Factory.Payment.Common().verifyNotify(params);

        } catch (Exception e) {
            logger.error("支付宝回调验签异常", e);
            return false;
        }
    }

    @Override
    public String getCallbackResponse(boolean success) {
        return success ? "success" : "failure";
    }

    @Override
    protected String formatAmount(String amount) {
        try {
            BigDecimal amt = new BigDecimal(amount);
            return amt.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
        } catch (Exception e) {
            logger.error("金额格式化失败: amount={}", amount, e);
            return "0.00";
        }
    }

    @Override
    protected String parseAmount(String amount) {
        try {
            return new BigDecimal(amount).toString();
        } catch (Exception e) {
            logger.error("金额解析失败: amount={}", amount, e);
            return "0";
        }
    }

    @Override
    protected String getConfig(String key) {
        switch (key) {
            case "app-id" :
                return appId;
            case "private-key" :
                return privateKey;
            case "alipay-public-key" :
                return alipayPublicKey;
            case "gateway-host" :
                return gatewayHost;
            case "sign-type" :
                return signType;
            default :
                return null;
        }
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(privateKey) && StringUtils.hasText(alipayPublicKey);
    }

    @Override
    public OrderStatus convertToOrderStatus(String platformStatus) {
        if (platformStatus == null || platformStatus.trim().isEmpty()) {
            logger.warn("支付宝平台状态为空，默认返回PENDING");
            return OrderStatus.PENDING;
        }

        String status = platformStatus.trim().toUpperCase();
        logger.debug("转换支付宝状态: {} -> 系统状态", status);

        switch (status) {
            // 支付成功状态
            case "TRADE_SUCCESS" :
            case "TRADE_FINISHED" :
                return OrderStatus.PAID;

            // 待支付状态
            case "WAIT_BUYER_PAY" :
                return OrderStatus.PENDING;

            // 交易关闭/取消状态
            case "TRADE_CLOSED" :
                return OrderStatus.CANCELLED;

            // 其他未知状态，默认为待支付
            default :
                logger.warn("未知的支付宝状态: {}，默认转换为PENDING", platformStatus);
                return OrderStatus.PENDING;
        }
    }

    @Override
    public String getProviderOrderIdForQuery(String systemOrderNo, String providerOrderId) {
        // 支付宝使用商户订单号进行查询，直接返回系统订单号
        return systemOrderNo;
    }
}