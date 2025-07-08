package org.xhy.infrastructure.utils;

import cn.hutool.crypto.digest.BCrypt;

/** 密码工具类 使用BCrypt算法进行密码加密和验证 */
public class PasswordUtils {

    private PasswordUtils() {
        throw new IllegalStateException("Utility class");
    }

    /** 加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码 */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    /** 验证密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
