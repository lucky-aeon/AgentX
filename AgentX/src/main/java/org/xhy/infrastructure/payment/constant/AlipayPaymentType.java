package org.xhy.infrastructure.payment.constant;

/** 支付宝支付类型常量 */
public class AlipayPaymentType {

    /** 网页支付 */
    public static final String WEB = "WEB";

    /** 扫码支付/二维码支付 */
    public static final String QR_CODE = "QR_CODE";

    private AlipayPaymentType() {
        // 私有构造函数，防止实例化
    }

    /** 检查是否为有效的支付类型 */
    public static boolean isValid(String paymentType) {
        return WEB.equals(paymentType) || QR_CODE.equals(paymentType);
    }

    /** 获取默认支付类型 */
    public static String getDefault() {
        return WEB;
    }
}