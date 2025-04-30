package org.xhy.infrastructure.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 配置加密工具 */
public class ConfigEncryptor {
    private static final String KEY = "AgentX-Config-Key";
    private static final AES aes = SecureUtil.aes(KEY.getBytes(StandardCharsets.UTF_8));

    /** 加密对象 */
    public static String encrypt(Object obj) {
        if (obj == null) {
            return null;
        }
        String json = JSON.toJSONString(obj);
        byte[] encrypted = aes.encrypt(json);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /** 解密对象 */
    public static <T> T decrypt(String encryptedStr, Class<T> clazz) {
        if (StringUtils.isBlank(encryptedStr)) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(encryptedStr);
        byte[] decrypted = aes.decrypt(decoded);
        String json = new String(decrypted, StandardCharsets.UTF_8);
        return JSON.parseObject(json, clazz);
    }
}