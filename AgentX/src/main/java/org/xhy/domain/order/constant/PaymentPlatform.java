package org.xhy.domain.order.constant;

/** 支付平台枚举 */
public enum PaymentPlatform {

    /** 支付宝 */
    ALIPAY("alipay", "支付宝"),

    /** 微信支付 */
    WECHAT("wechat", "微信支付"),

    /** Stripe */
    STRIPE("stripe", "Stripe");

    private final String code;
    private final String name;

    PaymentPlatform(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** 根据代码获取枚举值 */
    public static PaymentPlatform fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (PaymentPlatform platform : values()) {
            if (platform.code.equals(code)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("未知的支付平台代码: " + code);
    }

    /** 检查是否支持退款 */
    public boolean supportsRefund() {
        return this == ALIPAY || this == STRIPE;
    }

    /** 检查是否为第三方支付 */
    public boolean isThirdParty() {
        return true; // 目前所有支付平台都是第三方
    }

    /** 检查是否支持二维码支付 */
    public boolean supportsQrCode() {
        return this == ALIPAY || this == WECHAT;
    }

    /** 检查是否支持网页支付 */
    public boolean supportsWeb() {
        return true; // 所有平台都支持网页支付
    }

    /** 检查是否支持移动端支付 */
    public boolean supportsMobile() {
        return true; // 所有平台都支持移动端支付
    }
}