package org.xhy.infrastructure.payment.constant;

/** 支付日志类型枚举 */
public enum PaymentLogType {

    /** 创建支付 */
    CREATE("CREATE", "创建支付"),

    /** 支付回调 */
    CALLBACK("CALLBACK", "支付回调"),

    /** 支付查询 */
    QUERY("QUERY", "支付查询"),

    /** 支付取消 */
    CANCEL("CANCEL", "支付取消"),

    /** 支付退款 */
    REFUND("REFUND", "支付退款"),

    /** 支付错误 */
    ERROR("ERROR", "支付错误"),

    /** 支付完成 */
    COMPLETE("COMPLETE", "支付完成");

    private final String code;
    private final String description;

    PaymentLogType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据代码获取枚举 */
    public static PaymentLogType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PaymentLogType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的支付日志类型: " + code);
    }

    /** 检查是否为错误类型 */
    public boolean isError() {
        return this == ERROR;
    }

    /** 检查是否为成功类型 */
    public boolean isSuccess() {
        return this == COMPLETE;
    }
}