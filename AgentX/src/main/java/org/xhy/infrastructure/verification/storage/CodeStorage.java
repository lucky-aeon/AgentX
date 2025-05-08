package org.xhy.infrastructure.verification.storage;

/** 验证码存储接口 定义验证码存储的基本操作，可以有多种实现（如内存存储、Redis存储等） */
public interface CodeStorage {
    /** 存储验证码
     * @param key 键值，通常是邮箱或手机号
     * @param code 验证码
     * @param expirationMillis 过期时间（毫秒） */
    void storeCode(String key, String code, long expirationMillis);

    /** 获取验证码
     * @param key 键值
     * @return 验证码，如果不存在或已过期返回null */
    String getCode(String key);

    /** 验证验证码
     * @param key 键值
     * @param code 待验证的验证码
     * @return 验证结果，true表示验证通过 */
    boolean verifyCode(String key, String code);

    /** 移除验证码
     * @param key 键值 */
    void removeCode(String key);

    /** 清理所有过期的验证码 */
    void cleanExpiredCodes();
}