package org.xhy.infrastructure.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.infrastructure.payment.model.PaymentCallback;
import org.xhy.infrastructure.payment.model.PaymentRequest;
import org.xhy.infrastructure.payment.model.PaymentResult;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Stripe支付提供商 */
@Component
public class StripeProvider extends PaymentProvider {

    private static final Logger logger = LoggerFactory.getLogger(StripeProvider.class);

    @Value("${payment.stripe.secret-key:}")
    private String secretKey;

    @Value("${payment.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${payment.stripe.webhook-secret:whsec_0B9dRT54GJBF1gldp6IgJsU1z9ccbg2y}")
    private String webhookSecret;

    private volatile boolean initialized = false;

    @Override
    protected boolean supportsRefund() {
        return true;
    }

    @Override
    protected boolean supportsCancellation() {
        return true;
    }

    private void initializeConfig() throws StripeException {
        if (!initialized && isConfigured()) {
            synchronized (this) {
                if (!initialized) {
                    Stripe.apiKey = this.secretKey;
                    initialized = true;
                    logger.info("Stripe SDK 初始化成功");
                }
            }
        } else if (!isConfigured()) {
            throw new IllegalStateException("Stripe配置不完整，请检查 secret-key 配置");
        }
    }

    @Override
    public PaymentPlatform getPaymentPlatform() {
        return PaymentPlatform.STRIPE;
    }

    @Override
    public String getProviderCode() {
        return "stripe";
    }

    @Override
    public String getProviderName() {
        return "Stripe";
    }

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        try {
            initializeConfig();
            request.validate();

            logger.info("创建Stripe Checkout会话: orderId={}, amount={}", request.getOrderId(), request.getAmount());

            // 创建Stripe Checkout Session
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(
                            request.getSuccessUrl() != null ? request.getSuccessUrl() : "https://example.com/success")
                    .setCancelUrl(
                            request.getCancelUrl() != null ? request.getCancelUrl() : "https://example.com/cancel")
                    .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(request.getCurrency().toLowerCase())
                                    .setUnitAmount(Long.parseLong(formatAmount(request.getAmount().toString())))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(request.getTitle()).setDescription(request.getDescription())
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("order_id", request.getOrderId()).putMetadata("order_no", request.getOrderNo())
                    .putMetadata("user_id", request.getUserId()).putMetadata("payment_id", request.getPaymentId());

            // 如果有异步通知URL，设置webhook
            if (StringUtils.hasText(request.getNotifyUrl())) {
                // Stripe会通过webhook发送事件，这里记录一下通知URL用于后续处理
                paramsBuilder.putMetadata("notify_url", request.getNotifyUrl());
            }

            Session session = Session.create(paramsBuilder.build());

            PaymentResult result = PaymentResult.success();
            result.setPaymentUrl(session.getUrl()); // 用户需要跳转到的Stripe支付页面
            result.setProviderOrderId(session.getId());
            result.setProviderPaymentId(session.getId());
            result.setPaymentMethod("STRIPE");
            result.setPaymentType("WEB");
            result.setStatus("pending");

            // 设置额外信息
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("session_id", session.getId());
            extraData.put("checkout_url", session.getUrl());
            result.setExtraData(extraData);

            logger.info("Stripe Checkout会话创建成功: orderId={}, sessionId={}, checkoutUrl={}", request.getOrderId(),
                    session.getId(), session.getUrl());
            return result;

        } catch (Exception e) {
            logger.error("Stripe支付创建异常: orderId={}", request.getOrderId(), e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    @Override
    public PaymentResult queryPayment(String providerOrderId) {
        try {
            initializeConfig();

            logger.info("查询Stripe Checkout会话状态: sessionId={}", providerOrderId);

            Session session = Session.retrieve(providerOrderId);

            PaymentResult result = new PaymentResult();
            result.setProviderOrderId(session.getId());
            result.setProviderPaymentId(session.getPaymentIntent());
            result.setPaymentMethod("STRIPE");
            result.setPaymentType("WEB");
            result.setStatus(session.getStatus());

            // 检查支付状态
            if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
                result.setSuccess(true);
                logger.info("Stripe支付成功: sessionId={}, status={}, paymentStatus={}", providerOrderId,
                        session.getStatus(), session.getPaymentStatus());
            } else {
                result.setSuccess(false);
                result.setErrorMessage("支付未成功, 状态: " + session.getStatus() + ", 支付状态: " + session.getPaymentStatus());
                logger.info("Stripe支付未成功: sessionId={}, status={}, paymentStatus={}", providerOrderId,
                        session.getStatus(), session.getPaymentStatus());
            }

            // 设置原始响应数据
            Map<String, Object> rawResponse = new HashMap<>();
            rawResponse.put("id", session.getId());
            rawResponse.put("status", session.getStatus());
            rawResponse.put("payment_status", session.getPaymentStatus());
            rawResponse.put("payment_intent", session.getPaymentIntent());
            rawResponse.put("amount_total", session.getAmountTotal());
            rawResponse.put("currency", session.getCurrency());
            rawResponse.put("metadata", session.getMetadata());
            rawResponse.put("created", session.getCreated());
            rawResponse.put("url", session.getUrl());
            result.setRawResponse(rawResponse);

            return result;

        } catch (Exception e) {
            logger.error("Stripe支付查询异常: sessionId={}", providerOrderId, e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    @Override
    public PaymentCallback handleCallback(HttpServletRequest request) {
        PaymentCallback callback = new PaymentCallback();

        try {
            logger.info("处理Stripe支付回调");

            // 读取原始JSON请求体
            String payload = readRequestBody(request);
            String signature = request.getHeader("Stripe-Signature");

            logger.debug("Stripe回调原始数据长度: {}, 签名存在: {}", payload != null ? payload.length() : 0, signature != null);

            if (payload == null || payload.trim().isEmpty()) {
                logger.warn("Stripe回调请求体为空");
                callback.setSignatureValid(false);
                callback.setPaymentSuccess(false);
                callback.setErrorMessage("请求体为空");
                return callback;
            }

            // 解析JSON数据
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> callbackData = objectMapper.readValue(payload, Map.class);

            // 添加原始数据用于签名验证
            callbackData.put("_raw_body", payload);
            callbackData.put("_stripe_signature", signature);

            callback.setRawData(callbackData);

            // 直接处理Stripe事件数据，不调用旧方法
            return processStripeEvent(callbackData, callback);

        } catch (Exception e) {
            logger.error("Stripe回调处理异常", e);
            callback.setSignatureValid(false);
            callback.setPaymentSuccess(false);
            callback.setErrorMessage("回调处理异常: " + e.getMessage());
            return callback;
        }
    }

    /** 读取HTTP请求体
     * 
     * @param request HTTP请求对象
     * @return 请求体内容 */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        try (InputStream inputStream = request.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 处理Stripe事件数据
     * 
     * @param eventData 事件数据
     * @param callback 回调对象
     * @return 处理后的回调对象 */
    private PaymentCallback processStripeEvent(Map<String, Object> eventData, PaymentCallback callback) {
        try {
            logger.info("解析Stripe事件数据: {}", eventData.keySet());

            // 验证签名
            boolean isValid = verifyStripeSignature(eventData);
            callback.setSignatureValid(isValid);

            if (!isValid) {
                logger.warn("Stripe回调验签失败");
                return callback;
            }

            // 解析事件类型和数据
            String eventType = (String) eventData.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = (Map<String, Object>) data.get("object");

            String sessionId = (String) sessionData.get("id");
            String status = (String) sessionData.get("status");
            String paymentStatus = (String) sessionData.get("payment_status");
            String paymentIntentId = (String) sessionData.get("payment_intent");
            Object amountTotal = sessionData.get("amount_total");
            String currency = (String) sessionData.get("currency");
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) sessionData.get("metadata");

            logger.info("Stripe事件详情: type={}, sessionId={}, status={}, paymentStatus={}, amountTotal={}", eventType,
                    sessionId, status, paymentStatus, amountTotal);

            // 设置回调数据
            callback.setProviderOrderId(sessionId);
            callback.setProviderPaymentId(paymentIntentId != null ? paymentIntentId : sessionId);
            callback.setCurrency(currency != null ? currency.toUpperCase() : "USD");
            callback.setPaymentMethod("STRIPE");

            if (metadata != null) {
                callback.setOrderNo(metadata.get("order_no"));
                logger.info("从metadata获取订单号: {}", metadata.get("order_no"));
            }

            if (amountTotal != null) {
                String amountStr = amountTotal.toString();
                callback.setAmount(new BigDecimal(parseAmount(amountStr)));
                logger.info("解析金额: {} -> {}", amountStr, callback.getAmount());
            }

            // 判断支付状态
            if ("checkout.session.completed".equals(eventType) && "complete".equals(status)
                    && "paid".equals(paymentStatus)) {
                callback.setPaymentSuccess(true);
                callback.setPaymentStatus("SUCCESS");
                logger.info("Stripe支付成功回调: sessionId={}, paymentIntentId={}, amount={}", sessionId, paymentIntentId,
                        callback.getAmount());
            } else {
                callback.setPaymentSuccess(false);
                callback.setPaymentStatus(status + "/" + paymentStatus);
                logger.info("Stripe支付未成功回调: sessionId={}, status={}, paymentStatus={}", sessionId, status,
                        paymentStatus);
            }

        } catch (Exception e) {
            logger.error("Stripe事件处理异常", e);
            callback.setSignatureValid(false);
            callback.setPaymentSuccess(false);
            callback.setErrorMessage("事件处理异常: " + e.getMessage());
        }

        return callback;
    }

    /** 验证Stripe签名
     * 
     * @param eventData 事件数据
     * @return 是否验证通过 */
    private boolean verifyStripeSignature(Map<String, Object> eventData) {
        try {
            if (!StringUtils.hasText(webhookSecret)) {
                logger.warn("Stripe webhook secret未配置，跳过签名验证");
                return true;
            }

            String payload = (String) eventData.get("_raw_body");
            String signature = (String) eventData.get("_stripe_signature");

            if (!StringUtils.hasText(payload) || !StringUtils.hasText(signature)) {
                logger.warn("Stripe回调缺少必要的签名数据");
                return false;
            }

            // 使用Stripe SDK验证webhook签名
            Webhook.constructEvent(payload, signature, webhookSecret);
            return true;

        } catch (Exception e) {
            logger.error("Stripe回调验签异常", e);
            return false;
        }
    }

    @Override
    public PaymentResult cancelPayment(String providerOrderId) {
        try {
            initializeConfig();

            logger.info("尝试取消Stripe Checkout会话: sessionId={}", providerOrderId);

            // 检查会话状态
            Session session = Session.retrieve(providerOrderId);

            if (!"open".equals(session.getStatus())) {
                logger.warn("Stripe Checkout会话无法取消，当前状态: {}", session.getStatus());
                return PaymentResult.failure("CANNOT_CANCEL", "当前会话状态无法取消: " + session.getStatus());
            }

            // Stripe Checkout Session 不支持直接取消，但如果有关联的PaymentIntent，可以尝试取消它
            if (StringUtils.hasText(session.getPaymentIntent())) {
                try {
                    PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
                    if (canCancelPaymentIntent(paymentIntent.getStatus())) {
                        PaymentIntent canceledIntent = paymentIntent.cancel();

                        PaymentResult result = PaymentResult.success();
                        result.setProviderOrderId(session.getId());
                        result.setProviderPaymentId(canceledIntent.getId());
                        result.setStatus("canceled");

                        logger.info("Stripe支付取消成功: sessionId={}, paymentIntentId={}", providerOrderId,
                                canceledIntent.getId());
                        return result;
                    }
                } catch (Exception e) {
                    logger.warn("无法取消关联的PaymentIntent: {}", session.getPaymentIntent(), e);
                }
            }

            // 如果无法取消，返回不支持的错误
            logger.warn("Stripe Checkout会话不支持取消操作，会话将在24小时后自动过期");
            return PaymentResult.failure("NOT_SUPPORTED", "Stripe Checkout会话不支持取消操作，会话将在24小时后自动过期");

        } catch (Exception e) {
            logger.error("Stripe支付取消异常: sessionId={}", providerOrderId, e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    private boolean canCancelPaymentIntent(String status) {
        return "requires_payment_method".equals(status) || "requires_confirmation".equals(status)
                || "requires_action".equals(status) || "processing".equals(status);
    }

    @Override
    public PaymentResult refundPayment(String providerOrderId, String refundAmount, String refundReason) {
        try {
            initializeConfig();

            logger.info("申请Stripe退款: providerOrderId={}, amount={}, reason={}", providerOrderId, refundAmount,
                    refundReason);

            RefundCreateParams params = RefundCreateParams.builder().setPaymentIntent(providerOrderId)
                    .setAmount(Long.parseLong(formatAmount(refundAmount)))
                    .putMetadata("reason", refundReason != null ? refundReason : "").build();

            Refund refund = Refund.create(params);

            if ("succeeded".equals(refund.getStatus())) {
                PaymentResult result = PaymentResult.success();
                result.setProviderOrderId(refund.getPaymentIntent());
                result.setProviderPaymentId(refund.getId());

                // 设置退款信息
                Map<String, Object> extraData = new HashMap<>();
                extraData.put("refundAmount", parseAmount(refund.getAmount().toString()));
                extraData.put("refundId", refund.getId());
                extraData.put("refundStatus", refund.getStatus());
                extraData.put("refundReason", refundReason);
                result.setExtraData(extraData);

                logger.info("Stripe退款成功: providerOrderId={}, refundAmount={}, refundId={}", providerOrderId,
                        parseAmount(refund.getAmount().toString()), refund.getId());
                return result;
            } else {
                logger.error("Stripe退款失败: providerOrderId={}, status={}", providerOrderId, refund.getStatus());
                return PaymentResult.failure("REFUND_FAILED", "退款失败, 状态: " + refund.getStatus());
            }

        } catch (Exception e) {
            logger.error("Stripe退款异常: providerOrderId={}", providerOrderId, e);
            return PaymentResult.failure("SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    @Override
    public OrderStatus convertToOrderStatus(String platformStatus) {
        if (platformStatus == null || platformStatus.trim().isEmpty()) {
            logger.warn("Stripe平台状态为空，默认返回PENDING");
            return OrderStatus.PENDING;
        }

        String status = platformStatus.trim().toLowerCase();
        logger.debug("转换Stripe状态: {} -> 系统状态", status);

        switch (status) {
            // Checkout Session状态 - 支付成功
            case "complete" :
                return OrderStatus.PAID;

            // Checkout Session状态 - 进行中
            case "open" :
                return OrderStatus.PENDING;

            // Checkout Session状态 - 过期
            case "expired" :
                return OrderStatus.EXPIRED;

            // PaymentIntent状态 - 支付成功（兼容查询PaymentIntent的情况）
            case "succeeded" :
                return OrderStatus.PAID;

            // PaymentIntent状态 - 待支付（兼容）
            case "requires_payment_method" :
            case "requires_confirmation" :
            case "requires_action" :
            case "processing" :
                return OrderStatus.PENDING;

            // 取消状态
            case "canceled" :
                return OrderStatus.CANCELLED;

            // 其他未知状态，默认为待支付
            default :
                logger.warn("未知的Stripe状态: {}，默认转换为PENDING", platformStatus);
                return OrderStatus.PENDING;
        }
    }

    @Override
    public String getCallbackResponse(boolean success) {
        return success ? "success" : "failure";
    }

    @Override
    protected String formatAmount(String amount) {
        try {
            // Stripe使用分为单位，需要将元转换为分
            BigDecimal amt = new BigDecimal(amount);
            return amt.multiply(new BigDecimal("100")).setScale(0, BigDecimal.ROUND_HALF_UP).toString();
        } catch (Exception e) {
            logger.error("金额格式化失败: amount={}", amount, e);
            return "0";
        }
    }

    @Override
    protected String parseAmount(String amount) {
        try {
            // 将Stripe的分转换为元
            BigDecimal amt = new BigDecimal(amount);
            return amt.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP).toString();
        } catch (Exception e) {
            logger.error("金额解析失败: amount={}", amount, e);
            return "0";
        }
    }

    @Override
    protected String getConfig(String key) {
        switch (key) {
            case "secret-key" :
                return secretKey;
            case "publishable-key" :
                return publishableKey;
            case "webhook-secret" :
                return webhookSecret;
            default :
                return null;
        }
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(secretKey);
    }

    @Override
    public String getProviderOrderIdForQuery(String systemOrderNo, String providerOrderId) {
        // Stripe需要使用session_id进行查询
        if (StringUtils.hasText(providerOrderId)) {
            return providerOrderId;
        }

        // 如果数据库中没有存储session_id，说明订单创建可能失败了
        logger.error("Stripe查询订单失败: 数据库中未找到session_id, systemOrderNo={}", systemOrderNo);
        throw new IllegalStateException("Stripe订单查询失败: 未找到对应的session_id，订单可能创建失败");
    }
}