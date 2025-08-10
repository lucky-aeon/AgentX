package org.xhy.infrastructure.payment.constant;

/** 支付方式枚举（支付类型） */
public enum PaymentMethod {

    /** 网页支付 */
    WEB,

    /** 二维码支付 */
    QR_CODE,

    /** 移动端支付 */
    MOBILE,

    /** H5支付 */
    H5,

    /** 小程序支付 */
    MINI_PROGRAM;

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