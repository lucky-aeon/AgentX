package org.xhy.domain.order.constant;

/** 支付类型枚举 */
public enum PaymentType {

    /** 网页支付 */
    WEB("WEB", "网页支付"),

    /** 二维码支付 */
    QR_CODE("QR_CODE", "二维码支付"),

    /** 移动端支付 */
    MOBILE("MOBILE", "移动端支付"),

    /** H5支付 */
    H5("H5", "H5支付"),

    /** 小程序支付 */
    MINI_PROGRAM("mini_program", "小程序支付");

    private final String code;
    private final String name;

    PaymentType(String code, String name) {
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
    public static PaymentType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        for (PaymentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        // 兼容大写格式
        if ("QR_CODE".equalsIgnoreCase(code)) {
            return QR_CODE;
        }
        if ("MINI_PROGRAM".equalsIgnoreCase(code)) {
            return MINI_PROGRAM;
        }

        throw new IllegalArgumentException("未知的支付类型代码: " + code);
    }

    /** 检查是否需要跳转到第三方页面 */
    public boolean requiresRedirect() {
        return this == WEB || this == H5;
    }

    /** 检查是否支持移动端 */
    public boolean isMobileCompatible() {
        return this == MOBILE || this == H5 || this == MINI_PROGRAM;
    }

    /** 检查是否支持桌面端 */
    public boolean isDesktopCompatible() {
        return this == WEB || this == QR_CODE;
    }

    /** 检查是否是扫码类型 */
    public boolean isQrCodeBased() {
        return this == QR_CODE;
    }
}