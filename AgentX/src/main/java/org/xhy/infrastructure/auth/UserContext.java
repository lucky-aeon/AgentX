package org.xhy.infrastructure.auth;

/** 用户上下文 用于存储当前线程的用户信息 */
public class UserContext {

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    /** 设置当前用户ID
     *
     * @param userId 用户ID */
    public static void setCurrentUserId(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    /** 获取当前用户ID
     *
     * @return 用户ID，如果未设置则返回null */
    public static String getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 清除当前用户信息 */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
