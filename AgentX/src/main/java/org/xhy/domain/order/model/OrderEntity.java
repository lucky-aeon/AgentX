package org.xhy.domain.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.domain.order.constant.PaymentType;
import org.xhy.infrastructure.converter.JsonConverter;
import org.xhy.infrastructure.converter.OrderStatusConverter;
import org.xhy.infrastructure.converter.OrderTypeConverter;
import org.xhy.infrastructure.converter.PaymentPlatformConverter;
import org.xhy.infrastructure.converter.PaymentTypeConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** 订单实体 */
@TableName(value = "orders", autoResultMap = true)
public class OrderEntity extends BaseEntity {

    /** 订单唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 订单号（唯一） */
    @TableField("order_no")
    private String orderNo;

    /** 订单类型 */
    @TableField(value = "order_type", typeHandler = OrderTypeConverter.class)
    private OrderType orderType;

    /** 订单标题 */
    @TableField("title")
    private String title;

    /** 订单描述 */
    @TableField("description")
    private String description;

    /** 订单金额 */
    @TableField("amount")
    private BigDecimal amount;

    /** 货币代码 */
    @TableField("currency")
    private String currency;

    /** 订单状态 */
    @TableField(value = "status", typeHandler = OrderStatusConverter.class)
    private OrderStatus status;

    /** 订单过期时间 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 支付完成时间 */
    @TableField("paid_at")
    private LocalDateTime paidAt;

    /** 取消时间 */
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    /** 退款时间 */
    @TableField("refunded_at")
    private LocalDateTime refundedAt;

    /** 退款金额 */
    @TableField("refund_amount")
    private BigDecimal refundAmount;

    /** 支付平台 */
    @TableField(value = "payment_platform", typeHandler = PaymentPlatformConverter.class)
    private PaymentPlatform paymentPlatform;

    /** 支付类型 */
    @TableField(value = "payment_type", typeHandler = PaymentTypeConverter.class)
    private PaymentType paymentType;

    /** 第三方支付平台的订单ID */
    @TableField("provider_order_id")
    private String providerOrderId;

    /** 订单扩展信息 */
    @TableField(value = "metadata", typeHandler = JsonConverter.class)
    private Map<String, Object> metadata;

    public OrderEntity() {
        this.currency = "CNY";
        this.status = OrderStatus.PENDING;
        this.refundAmount = BigDecimal.ZERO;
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

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    /** 生成订单号 */
    public void generateOrderNo() {
        if (this.orderNo == null) {
            this.orderNo = "ORD" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    /** 设置订单过期时间（默认30分钟后过期） */
    public void setDefaultExpiration() {
        if (this.expiredAt == null) {
            this.expiredAt = LocalDateTime.now().plusMinutes(30);
        }
    }

    /** 检查订单是否过期 */
    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }

    /** 标记订单为已支付 */
    public void markAsPaid() {
        if (!status.canPay()) {
            throw new BusinessException("订单状态不允许支付操作");
        }
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /** 取消订单 */
    public void cancel() {
        if (!status.canCancel()) {
            throw new BusinessException("订单状态不允许取消操作");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /** 退款订单 */
    public void refund(BigDecimal refundAmount) {
        if (!status.canRefund()) {
            throw new BusinessException("订单状态不允许退款操作");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("退款金额必须大于0");
        }
        if (refundAmount.compareTo(amount) > 0) {
            throw new BusinessException("退款金额不能超过订单金额");
        }

        this.status = OrderStatus.REFUNDED;
        this.refundAmount = refundAmount;
        this.refundedAt = LocalDateTime.now();
    }

    /** 标记订单为已过期 */
    public void markAsExpired() {
        if (status.isFinished()) {
            return; // 已完成的订单不能标记为过期
        }
        this.status = OrderStatus.EXPIRED;
    }

    /** 验证订单数据 */
    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (orderType == null) {
            throw new BusinessException("订单类型不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("订单标题不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订单金额必须大于0");
        }
        if (currency == null || currency.trim().isEmpty()) {
            this.currency = "CNY";
        }
        if (status == null) {
            this.status = OrderStatus.PENDING;
        }
        if (refundAmount == null) {
            this.refundAmount = BigDecimal.ZERO;
        }
    }

    /** 创建新订单 */
    public static OrderEntity createNew(String userId, OrderType orderType, String title, String description,
            BigDecimal amount) {
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setOrderType(orderType);
        order.setTitle(title);
        order.setDescription(description);
        order.setAmount(amount);
        order.generateOrderNo();
        order.setDefaultExpiration();
        order.validate();
        return order;
    }

    /** 创建新订单（包含支付信息） */
    public static OrderEntity createNew(String userId, OrderType orderType, String title, String description,
            BigDecimal amount, PaymentPlatform paymentPlatform, PaymentType paymentType) {
        OrderEntity order = createNew(userId, orderType, title, description, amount);
        order.setPaymentPlatform(paymentPlatform);
        order.setPaymentType(paymentType);
        return order;
    }

    /** 创建充值订单 */
    public static OrderEntity createRechargeOrder(String userId, BigDecimal amount, String remark) {
        String title = "账户充值";
        String description = remark != null && !remark.trim().isEmpty() ? remark : "充值 ¥" + amount + " 到账户余额";
        return createNew(userId, OrderType.RECHARGE, title, description, amount);
    }

    /** 创建充值订单（包含支付信息） */
    public static OrderEntity createRechargeOrder(String userId, BigDecimal amount, String remark,
            PaymentPlatform paymentPlatform, PaymentType paymentType) {
        String title = "账户充值";
        String description = remark != null && !remark.trim().isEmpty() ? remark : "充值 ¥" + amount + " 到账户余额";
        return createNew(userId, OrderType.RECHARGE, title, description, amount, paymentPlatform, paymentType);
    }

    /** 创建购买订单 */
    public static OrderEntity createPurchaseOrder(String userId, String productName, BigDecimal amount,
            String description) {
        String title = "购买 " + productName;
        return createNew(userId, OrderType.PURCHASE, title, description, amount);
    }

    /** 创建购买订单（包含支付信息） */
    public static OrderEntity createPurchaseOrder(String userId, String productName, BigDecimal amount,
            String description, PaymentPlatform paymentPlatform, PaymentType paymentType) {
        String title = "购买 " + productName;
        return createNew(userId, OrderType.PURCHASE, title, description, amount, paymentPlatform, paymentType);
    }
}