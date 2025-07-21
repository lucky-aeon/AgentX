package org.xhy.infrastructure.utils;

import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.exception.ParamValidationException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.regex.Pattern;

/** 参数校验工具类 */
public class ValidationUtils {

    // 简化版语义化版本格式，例如 1.0.0
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    /** 校验参数不为空 */
    public static void notNull(Object value, String paramName) {
        if (value == null) {
            throw new ParamValidationException(paramName, "不能为空");
        }
    }

    /** 校验字符串不为空 */
    public static void notEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValidationException(paramName, "不能为空");
        }
    }

    /** 校验集合不为空 */
    public static void notEmpty(Collection<?> collection, String paramName) {
        if (collection == null || collection.isEmpty()) {
            throw new ParamValidationException(paramName, "不能为空");
        }
    }

    /** 校验字符串长度 */
    public static void length(String value, int min, int max, String paramName) {
        if (value == null) {
            throw new ParamValidationException(paramName, "不能为空");
        }

        int length = value.length();
        if (length < min || length > max) {
            throw new ParamValidationException(paramName, String.format("长度必须在%d-%d之间，当前长度: %d", min, max, length));
        }
    }

    /** 校验数值范围 */
    public static void range(int value, int min, int max, String paramName) {
        if (value < min || value > max) {
            throw new ParamValidationException(paramName, String.format("必须在%d-%d之间，当前值: %d", min, max, value));
        }
    }

    /** 校验版本号格式是否正确 */
    public static void validVersionFormat(String version, String paramName) {
        notEmpty(version, paramName);
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new ParamValidationException(paramName, "版本号格式不正确，应为 X.Y.Z 格式，例如 1.0.0");
        }
    }

    /** 加密工具类 */
    public static class EncryptUtils {

        private static final String ALGORITHM = "AES";
        private static final String SECRET_KEY = "1234567890123456"; // 16位密钥

        private EncryptUtils() {
            // 私有构造函数，防止实例化
        }

        /** 加密字符串
         *
         * @param data 待加密的字符串
         * @return 加密后的字符串 */
        public static String encrypt(String data) {
            try {
                if (data == null) {
                    return null;
                }
                SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedBytes = cipher.doFinal(data.getBytes());
                return Base64.getEncoder().encodeToString(encryptedBytes);
            } catch (Exception e) {
                throw new BusinessException("加密失败" + e.getMessage(), e);
            }
        }

        /** 解密字符串
         *
         * @param encryptedData 已加密的字符串
         * @return 解密后的字符串 */
        public static String decrypt(String encryptedData) {
            try {
                if (encryptedData == null) {
                    return null;
                }
                SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
                return new String(decryptedBytes);
            } catch (Exception e) {
                throw new BusinessException("解密失败:" + e.getMessage(), e);
            }
        }
    }
}