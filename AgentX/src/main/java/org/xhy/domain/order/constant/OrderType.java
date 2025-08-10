package org.xhy.domain.order.constant;

/** 订单类型枚举 */
public enum OrderType {

    /** 充值订单 */
    RECHARGE("RECHARGE", "账户充值"),

    /** 购买订单 */
    PURCHASE("PURCHASE", "商品购买"),

    /** 订阅订单 */
    SUBSCRIPTION("SUBSCRIPTION", "服务订阅"),

    /** 续费订单 */
    RENEWAL("RENEWAL", "服务续费");

    private final String code;
    private final String description;

    OrderType(String code, String description) {
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
    public static OrderType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OrderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的订单类型: " + code);
    }

    /** 检查是否为充值类订单 */
    public boolean isRechargeType() {
        return this == RECHARGE;
    }

    /** 检查是否为购买类订单 */
    public boolean isPurchaseType() {
        return this == PURCHASE || this == SUBSCRIPTION || this == RENEWAL;
    }
}