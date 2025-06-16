package org.xhy.infrastructure.auth;

/** 外部API上下文管理 用于在ThreadLocal中存储通过API Key验证后的用户信息 */
public class ExternalApiContext {

    private static final ThreadLocal<String> userId = new ThreadLocal<>();
    private static final ThreadLocal<String> agentId = new ThreadLocal<>();

    /** 设置用户ID */
    public static void setUserId(String userId) {
        ExternalApiContext.userId.set(userId);
    }

    /** 获取用户ID */
    public static String getUserId() {
        return userId.get();
    }

    /** 设置AgentID */
    public static void setAgentId(String agentId) {
        ExternalApiContext.agentId.set(agentId);
    }

    /** 获取AgentID */
    public static String getAgentId() {
        return agentId.get();
    }

    /** 检查是否已设置用户ID */
    public static boolean hasUserId() {
        return userId.get() != null;
    }

    /** 检查是否已设置AgentID */
    public static boolean hasAgentId() {
        return agentId.get() != null;
    }

    /** 清理上下文，避免内存泄漏 */
    public static void clear() {
        userId.remove();
        agentId.remove();
    }
}