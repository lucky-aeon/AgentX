package org.xhy.domain.rule.constant;

/** 规则处理器标识枚举 对应计费策略类的标识 */
public enum RuleHandlerKey {

    /** 模型Token计费策略 */
    MODEL_TOKEN_STRATEGY("MODEL_TOKEN_STRATEGY", "模型Token计费策略"),

    /** 按次计费策略 - 用于Agent创建等固定单价场景 */
    PER_UNIT_STRATEGY("PER_UNIT_STRATEGY", "按次计费策略"),

    /** 分层计费策略 */
    TIERED_STRATEGY("TIERED_STRATEGY", "分层计费策略"),

    /** 按量阶梯计费策略 */
    VOLUME_TIERED_STRATEGY("VOLUME_TIERED_STRATEGY", "按量阶梯计费策略");

    private final String key;
    private final String description;

    RuleHandlerKey(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    /** 根据key获取枚举 */
    public static RuleHandlerKey fromKey(String key) {
        for (RuleHandlerKey handlerKey : values()) {
            if (handlerKey.key.equals(key)) {
                return handlerKey;
            }
        }
        throw new IllegalArgumentException("未知的规则处理器标识: " + key);
    }

    /** 检查key是否有效 */
    public static boolean isValidKey(String key) {
        try {
            fromKey(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}