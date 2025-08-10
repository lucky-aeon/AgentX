package org.xhy.domain.order.constant;

/** 订单状态枚举 */
public enum OrderStatus {

    /** 待支付 */
    PENDING(1, "待支付"),

    /** 已支付 */
    PAID(2, "已支付"),

    /** 已取消 */
    CANCELLED(3, "已取消"),

    /** 已退款 */
    REFUNDED(4, "已退款"),

    /** 已过期 */
    EXPIRED(5, "已过期");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据状态码获取枚举 */
    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态码: " + code);
    }

    /** 检查订单是否为已完成状态 */
    public boolean isCompleted() {
        return this == PAID || this == REFUNDED;
    }

    /** 检查订单是否为结束状态（不可再操作） */
    public boolean isFinished() {
        return this == PAID || this == CANCELLED || this == REFUNDED || this == EXPIRED;
    }

    /** 检查订单是否可以支付 */
    public boolean canPay() {
        return this == PENDING;
    }

    /** 检查订单是否可以取消 */
    public boolean canCancel() {
        return this == PENDING;
    }

    /** 检查订单是否可以退款 */
    public boolean canRefund() {
        return this == PAID;
    }
}