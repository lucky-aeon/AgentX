package org.xhy.infrastructure.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密工具类
 */
public class EncryptUtils {
    
    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = "1234567890123456"; // 16位密钥
    
    private EncryptUtils() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 加密字符串
     *
     * @param data 待加密的字符串
     * @return 加密后的字符串
     */
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
            throw new RuntimeException("加密失败"+e.getMessage(), e);
        }
    }
    
    /**
     * 解密字符串
     *
     * @param encryptedData 已加密的字符串
     * @return 解密后的字符串
     */
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
            throw new RuntimeException("解密失败:"+e.getMessage(), e);
        }
    }
} 