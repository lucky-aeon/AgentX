package org.xhy.infrastructure.payment.constant;

/** 支付状态枚举 */
public enum PaymentStatus {

    /** 已创建 */
    CREATED,

    /** 等待支付 */
    PENDING,

    /** 支付成功 */
    SUCCESS,

    /** 支付失败 */
    FAILED,

    /** 已取消 */
    CANCELLED,

    /** 已过期 */
    EXPIRED;

    /** 检查支付是否成功 */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /** 检查支付是否失败 */
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED || this == EXPIRED;
    }

    /** 检查支付是否进行中 */
    public boolean isPending() {
        return this == CREATED || this == PENDING;
    }

    /** 检查支付是否结束（不可再操作） */
    public boolean isFinished() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == EXPIRED;
    }

    /** 检查是否可以取消支付 */
    public boolean canCancel() {
        return this == CREATED || this == PENDING;
    }
}