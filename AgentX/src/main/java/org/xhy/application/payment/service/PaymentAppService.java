package org.xhy.application.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.xhy.application.payment.assembler.PaymentAssembler;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.domain.order.constant.PaymentType;
import org.xhy.domain.order.event.PurchaseSuccessEvent;
import org.xhy.domain.order.model.OrderEntity;
import org.xhy.domain.order.service.OrderDomainService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.payment.constant.PaymentMethod;
import org.xhy.infrastructure.ratelimit.service.RateLimitService;
import org.xhy.infrastructure.payment.factory.PaymentProviderFactory;
import org.xhy.infrastructure.payment.model.PaymentCallback;
import org.xhy.infrastructure.payment.model.PaymentRequest;
import org.xhy.infrastructure.payment.model.PaymentResult;
import org.xhy.infrastructure.payment.provider.PaymentProvider;
import org.xhy.interfaces.dto.account.request.RechargeRequest;
import org.xhy.interfaces.dto.account.response.PaymentResponseDTO;
import org.xhy.interfaces.dto.account.response.OrderStatusResponseDTO;
import org.xhy.interfaces.dto.account.response.PaymentMethodDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/** 支付应用服务 */
@Service
public class PaymentAppService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentAppService.class);

    private final OrderDomainService orderDomainService;
    private final PaymentProviderFactory paymentProviderFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimitService rateLimitService;

    public PaymentAppService(OrderDomainService orderDomainService, PaymentProviderFactory paymentProviderFactory,
            ApplicationEventPublisher eventPublisher, RateLimitService rateLimitService) {
        this.orderDomainService = orderDomainService;
        this.paymentProviderFactory = paymentProviderFactory;
        this.eventPublisher = eventPublisher;
        this.rateLimitService = rateLimitService;
    }

    /** 创建充值订单并发起支付
     * 
     * @param request 充值请求
     * @return 支付响应 */
    @Transactional
    public PaymentResponseDTO createRechargePayment(RechargeRequest request) {
        String userId = UserContext.getCurrentUserId();

        // 限流检查
        rateLimitService.checkRechargeRateLimit(userId);

        // 转换支付平台和类型
        PaymentPlatform paymentPlatform = PaymentPlatform.fromCode(request.getPaymentPlatform());
        PaymentType paymentType = PaymentType.fromCode(request.getPaymentType());

        if (!paymentProviderFactory.isAvailable(paymentPlatform)) {
            throw new BusinessException("支付平台暂不可用: " + paymentPlatform.getName());
        }

        // 创建充值订单
        OrderEntity order = createRechargeOrder(userId, request, paymentPlatform, paymentType);

        try {
            // 发起支付
            PaymentResult paymentResult = createPaymentWithProvider(order, request, paymentPlatform);

            if (!paymentResult.isSuccess()) {
                logger.error("充值支付创建失败: userId={}, orderId={}, error={}", userId, order.getId(),
                        paymentResult.getErrorMessage());
                throw new BusinessException("支付创建失败: " + paymentResult.getErrorMessage());
            }

            // 更新订单的支付平台信息
            updateOrderProviderInfo(order, paymentResult);

            // 构建并返回响应
            PaymentResponseDTO response = PaymentAssembler.toPaymentResponseDTO(order, paymentResult);

            logger.info(
                    "充值支付创建成功: userId={}, orderId={}, amount={}, platform={}, type={}, providerOrderId={}, providerPaymentId={}",
                    userId, order.getId(), request.getAmount(), paymentPlatform, paymentType,
                    order.getProviderOrderId(), order.getProviderOrderId());

            return response;

        } catch (Exception e) {
            logger.error("充值支付处理异常: userId={}, orderId={}", userId, order.getId(), e);
            throw new BusinessException("支付处理失败: " + e.getMessage());
        }
    }

    /** 创建充值订单 */
    private OrderEntity createRechargeOrder(String userId, RechargeRequest request, PaymentPlatform paymentPlatform,
            PaymentType paymentType) {
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setOrderType(OrderType.RECHARGE);
        order.setTitle("账户充值");
        order.setDescription("账户余额充值 ¥" + request.getAmount());
        order.setAmount(request.getAmount());
        order.setCurrency("CNY");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentPlatform(paymentPlatform);
        order.setPaymentType(paymentType);

        // 设置订单元数据，包含业务相关信息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessType", "balance_recharge"); // 业务类型：余额充值
        metadata.put("rechargeAmount", request.getAmount().toString()); // 充值金额
        metadata.put("paymentPlatform", paymentPlatform.getCode()); // 支付平台
        metadata.put("paymentType", paymentType.getCode()); // 支付类型

        // 如果有备注信息，也保存到metadata中
        if (request.getRemark() != null && !request.getRemark().trim().isEmpty()) {
            metadata.put("remark", request.getRemark().trim());
            // 更新订单描述，包含备注
            order.setDescription("账户余额充值 ¥" + request.getAmount() + " - " + request.getRemark().trim());
        }

        order.setMetadata(metadata);
        // 订单创建时间由BaseEntity自动设置

        return orderDomainService.createOrder(order);
    }

    /** 通过支付提供商创建支付 */
    private PaymentResult createPaymentWithProvider(OrderEntity order, RechargeRequest request,
            PaymentPlatform paymentPlatform) {
        PaymentProvider provider = paymentProviderFactory.getProvider(paymentPlatform);
        PaymentRequest paymentRequest = buildPaymentRequest(order, request);
        return provider.createPayment(paymentRequest);
    }

    /** 更新订单的支付平台信息 */
    private void updateOrderProviderInfo(OrderEntity order, PaymentResult paymentResult) {
        // 只有当有支付平台信息时才更新
        if (paymentResult.getProviderOrderId() == null && paymentResult.getProviderPaymentId() == null) {
            return;
        }

        logger.info("更新订单的支付平台信息: orderId={}, providerOrderId={}, providerPaymentId={}", order.getId(),
                paymentResult.getProviderOrderId(), paymentResult.getProviderPaymentId());

        // 更新数据库中的订单信息
        orderDomainService.updateOrderStatusAndProviderInfo(order.getId(), order.getStatus(), // 保持当前状态
                paymentResult.getProviderOrderId());

        // 同时更新内存中的订单对象
        if (paymentResult.getProviderOrderId() != null) {
            order.setProviderOrderId(paymentResult.getProviderOrderId());
        }

    }

    /** 构建支付请求 */
    private PaymentRequest buildPaymentRequest(OrderEntity order, RechargeRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(order.getId());
        paymentRequest.setPaymentId(order.getId()); // 使用订单ID作为支付ID
        paymentRequest.setOrderNo(order.getOrderNo());
        paymentRequest.setTitle(order.getTitle());
        paymentRequest.setDescription(order.getDescription());
        paymentRequest.setAmount(order.getAmount());
        paymentRequest.setCurrency(order.getCurrency());
        paymentRequest.setUserId(order.getUserId());
        paymentRequest.setPaymentType(order.getPaymentType().getCode());

        // 设置回调URL，使用平台代码作为路径参数
        String platformCode = order.getPaymentPlatform().getCode();
        paymentRequest.setNotifyUrl(
                "https://www.bilibili.com/video/BV1nL8NzkEaH/?spm_id_from=333.1007.tianma.1-3-3.click&vd_source=884a1f9702167e8936a8d6d773a193ae/api/payments/callback/"
                        + platformCode);
        paymentRequest.setSuccessUrl(
                "https://www.bilibili.com/video/BV1nL8NzkEaH/?spm_id_from=333.1007.tianma.1-3-3.click&vd_source=884a1f9702167e8936a8d6d773a193ae/api/payments/success");
        paymentRequest.setCancelUrl(
                "https://www.bilibili.com/video/BV1nL8NzkEaH/?spm_id_from=333.1007.tianma.1-3-3.click&vd_source=884a1f9702167e8936a8d6d773a193ae/api/payments/cancel");

        return paymentRequest;
    }

    /** 构建支付请求（仅用于查询） */
    private PaymentRequest buildPaymentRequest(OrderEntity order) {
        return buildPaymentRequest(order, null);
    }

    /** 生成订单号 */
    private String generateOrderNo() {
        return "RCH" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }

    /** 处理支付回调（新接口，直接处理HTTP请求）
     * 
     * @param paymentPlatform 支付平台代码
     * @param request HTTP请求对象
     * @return 回调响应字符串 */
    @Transactional
    public String handlePaymentCallback(PaymentPlatform paymentPlatform, HttpServletRequest request) {
        try {
            // 获取支付提供商
            PaymentProvider provider = paymentProviderFactory.getProvider(paymentPlatform);

            // 使用新的处理方式：直接传递HttpServletRequest
            PaymentCallback callback = provider.handleCallback(request);

            if (callback.isSignatureValid()) {
                // 更新订单状态
                updateOrderStatus(callback);

                logger.info("支付回调处理成功: platform={}, orderNo={}, success={}", paymentPlatform, callback.getOrderNo(),
                        callback.isPaymentSuccess());
            } else {
                logger.warn("支付回调验签失败: platform={}, orderNo={}", paymentPlatform, callback.getOrderNo());
            }

            // 返回平台要求的响应格式
            return provider.getCallbackResponse(callback.isSignatureValid() && callback.isPaymentSuccess());

        } catch (Exception e) {
            logger.error("支付回调处理异常: platform={}", paymentPlatform, e);
            // 返回失败响应，避免重复回调
            return "failure";
        }
    }

    /** 更新订单状态
     * 
     * @param callback 支付回调对象 */
    private void updateOrderStatus(PaymentCallback callback) {
        try {
            String orderNo = callback.getOrderNo();
            if (orderNo == null || orderNo.trim().isEmpty()) {
                logger.warn("回调中没有订单号信息");
                return;
            }

            // 根据订单号获取订单
            OrderEntity order = orderDomainService.getOrderByOrderNo(orderNo);
            if (order == null) {
                logger.warn("订单不存在: orderNo={}", orderNo);
                return;
            }

            // 检查订单状态是否可以更新
            if (order.getStatus() != OrderStatus.PENDING) {
                logger.info("订单状态已更新，跳过处理: orderNo={}, currentStatus={}", orderNo, order.getStatus());
                return;
            }

            // 根据支付结果更新订单状态
            OrderStatus newStatus;
            if (callback.isPaymentSuccess()) {
                newStatus = OrderStatus.PAID;
                logger.info("订单支付成功: orderNo={}, amount={}, providerOrderId={}, providerPaymentId={}", orderNo,
                        callback.getAmount(), callback.getProviderOrderId(), callback.getProviderPaymentId());
            } else {
                newStatus = OrderStatus.CANCELLED;
                logger.info("订单支付失败: orderNo={}", orderNo);
            }

            // 同时更新订单状态和支付平台信息
            orderDomainService.updateOrderStatusAndProviderInfo(order.getId(), newStatus,
                    callback.getProviderOrderId());

            // 更新内存中的订单对象
            order.setStatus(newStatus);
            if (callback.getProviderOrderId() != null) {
                order.setProviderOrderId(callback.getProviderOrderId());
            }
            if (callback.getProviderPaymentId() != null) {
                order.setProviderOrderId(callback.getProviderPaymentId());
            }

            // 如果订单支付成功，发布购买成功事件
            if (newStatus == OrderStatus.PAID) {
                logger.info("订单支付成功，发布购买成功事件: orderNo={}, orderType={}, amount={}", orderNo, order.getOrderType(),
                        order.getAmount());
                PurchaseSuccessEvent event = new PurchaseSuccessEvent(order);
                eventPublisher.publishEvent(event);
            }

        } catch (Exception e) {
            logger.error("更新订单状态失败: orderNo={}", callback.getOrderNo(), e);
            throw new BusinessException("订单状态更新失败: " + e.getMessage());
        }
    }

    /** 查询订单状态（根据订单号）
     * 
     * @param orderNo 订单号
     * @return 订单状态响应 */
    public OrderStatusResponseDTO queryOrderStatus(String orderNo) {
        logger.info("查询订单状态: orderNo={}", orderNo);

        try {
            // 1. 获取订单
            OrderEntity order = getOrderOrThrow(orderNo);

            // 2. 同步支付平台状态（如果需要）
            if (shouldSyncWithProvider(order)) {
                syncOrderStatusFromProvider(order);
            }

            // 3. 构建响应
            return buildOrderStatusResponse(order);

        } catch (Exception e) {
            logger.error("查询订单状态失败: orderNo={}", orderNo, e);
            throw new BusinessException("查询订单状态失败: " + e.getMessage());
        }
    }

    /** 获取订单或抛出异常 */
    private OrderEntity getOrderOrThrow(String orderNo) {
        OrderEntity order = orderDomainService.findOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderNo);
        }
        return order;
    }

    /** 判断是否需要同步支付平台状态 */
    private boolean shouldSyncWithProvider(OrderEntity order) {
        return order.getStatus() == OrderStatus.PENDING;
    }

    /** 获取用于查询的第三方平台订单ID */
    private String getProviderOrderIdForQuery(PaymentProvider provider, OrderEntity order) {
        // 让支付提供商自己决定使用哪个ID进行查询
        return provider.getProviderOrderIdForQuery(order.getOrderNo(), order.getProviderOrderId());
    }

    /** 同步支付平台状态 */
    private void syncOrderStatusFromProvider(OrderEntity order) {
        try {
            PaymentProvider provider = paymentProviderFactory.getProvider(order.getPaymentPlatform());

            // 获取第三方平台的订单ID
            String providerOrderId = getProviderOrderIdForQuery(provider, order);

            PaymentResult platformResult = provider.queryPayment(providerOrderId);

            if (!platformResult.isSuccess() && platformResult.getStatus() == null) {
                logger.warn("查询支付平台订单状态失败: orderNo={}, error={}", order.getOrderNo(), platformResult.getErrorMessage());
                return;
            }
            // 转换支付平台状态
            OrderStatus platformStatus = provider.convertToOrderStatus(platformResult.getStatus());

            if (platformStatus == order.getStatus()) {
                return;
            }

            // 更新订单状态和信息
            updateOrderWithProviderResult(order, platformStatus, platformResult);

            // 发布支付成功事件（如果需要）
            publishPaymentSuccessEventIfNeeded(order, platformStatus);

        } catch (Exception e) {
            logger.warn("查询支付平台订单状态异常: orderNo={}", order.getOrderNo(), e);
            // 平台查询异常时，不影响返回本地订单状态
        }
    }

    /** 使用支付平台结果更新订单 */
    private void updateOrderWithProviderResult(OrderEntity order, OrderStatus newStatus, PaymentResult platformResult) {
        OrderStatus oldStatus = order.getStatus();

        logger.info(
                "订单状态不一致，更新本地状态: orderNo={}, localStatus={}, platformStatus={}, rawStatus={}, providerOrderId={}, providerPaymentId={}",
                order.getOrderNo(), oldStatus, newStatus, platformResult.getStatus(),
                platformResult.getProviderOrderId(), platformResult.getProviderPaymentId());

        // 更新数据库中的订单状态和支付平台信息
        orderDomainService.updateOrderStatusAndProviderInfo(order.getId(), newStatus,
                platformResult.getProviderOrderId());

        // 更新内存中的订单对象
        order.setStatus(newStatus);
        if (platformResult.getProviderOrderId() != null) {
            order.setProviderOrderId(platformResult.getProviderOrderId());
        }
    }

    /** 如果支付成功则发布支付成功事件 */
    private void publishPaymentSuccessEventIfNeeded(OrderEntity order, OrderStatus status) {
        if (status != OrderStatus.PAID) {
            return;
        }

        logger.info("订单支付成功，发布购买成功事件: orderNo={}, orderType={}, amount={}, providerOrderId={}, providerPaymentId={}",
                order.getOrderNo(), order.getOrderType(), order.getAmount(), order.getProviderOrderId(),
                order.getProviderOrderId());

        PurchaseSuccessEvent event = new PurchaseSuccessEvent(order);
        eventPublisher.publishEvent(event);
    }

    /** 构建订单状态响应 */
    private OrderStatusResponseDTO buildOrderStatusResponse(OrderEntity order) {
        OrderStatusResponseDTO response = new OrderStatusResponseDTO();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setPaymentPlatform(order.getPaymentPlatform());
        response.setPaymentType(order.getPaymentType());
        response.setAmount(order.getAmount());
        response.setTitle(order.getTitle());

        response.setCreatedAt(order.getCreatedAt().toString());
        response.setUpdatedAt(order.getUpdatedAt().toString());
        if (order.getExpiredAt() != null) {
            response.setExpiredAt(order.getExpiredAt().toString());
        }

        return response;
    }

    /** 获取可用的支付方法列表
     * 
     * @return 支付方法列表 */
    @Transactional(readOnly = true)
    public List<PaymentMethodDTO> getAvailablePaymentMethods() {
        logger.info("获取可用的支付方法列表");

        List<PaymentMethodDTO> methods = new ArrayList<>();

        try {
            // 获取所有可用的支付平台
            List<PaymentPlatform> availablePlatforms = paymentProviderFactory.getAvailablePaymentPlatforms();

            for (PaymentPlatform platform : availablePlatforms) {
                PaymentMethodDTO methodDTO = new PaymentMethodDTO();
                methodDTO.setPlatformCode(platform.getCode());
                methodDTO.setPlatformName(platform.getName());
                methodDTO.setAvailable(true);
                methodDTO.setDescription(getPaymentPlatformDescription(platform));

                // 获取该平台支持的支付类型
                List<PaymentMethodDTO.PaymentTypeDTO> paymentTypes = getSupportedPaymentTypes(platform);
                methodDTO.setPaymentTypes(paymentTypes);

                methods.add(methodDTO);
            }

            logger.info("获取支付方法列表成功: 共{}个平台", methods.size());
            return methods;

        } catch (Exception e) {
            logger.error("获取支付方法列表失败", e);
            // 返回空列表而不是抛出异常，避免影响前端页面
            return new ArrayList<>();
        }
    }

    /** 获取支付平台描述 */
    private String getPaymentPlatformDescription(PaymentPlatform platform) {
        switch (platform) {
            case ALIPAY :
                return "支持扫码支付";
            case STRIPE :
                return "支持信用卡支付";
            case WECHAT :
                return "支持扫码支付";
            default :
                return platform.getName() + "支付";
        }
    }

    /** 获取平台支持的支付类型 */
    private List<PaymentMethodDTO.PaymentTypeDTO> getSupportedPaymentTypes(PaymentPlatform platform) {
        List<PaymentMethodDTO.PaymentTypeDTO> types = new ArrayList<>();

        // 只支持二维码支付
        switch (platform) {
            case ALIPAY :
                types.add(new PaymentMethodDTO.PaymentTypeDTO("QR_CODE", "扫码支付", false));
                break;
            case WECHAT :
                types.add(new PaymentMethodDTO.PaymentTypeDTO("QR_CODE", "扫码支付", false));
                break;
            case STRIPE :
                // Stripe暂不支持二维码支付，跳过
                break;
            default :
                // 其他平台暂不支持，跳过
                break;
        }

        // 为每个支付类型设置描述
        for (PaymentMethodDTO.PaymentTypeDTO type : types) {
            type.setDescription(getPaymentTypeDescription(type.getTypeCode()));
        }

        return types;
    }

    /** 获取支付类型描述 */
    private String getPaymentTypeDescription(String typeCode) {
        switch (typeCode) {
            case "WEB" :
                return "跳转到支付平台网页完成支付";
            case "QR_CODE" :
                return "扫描二维码完成支付";
            case "MOBILE" :
                return "移动端应用内支付";
            case "H5" :
                return "移动端网页支付";
            case "MINI_PROGRAM" :
                return "小程序内支付";
            default :
                return "在线支付";
        }
    }

}