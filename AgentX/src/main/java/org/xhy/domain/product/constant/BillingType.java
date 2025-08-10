package org.xhy.domain.product.constant;

/** 计费类型枚举 */
public enum BillingType {

    /** 模型调用计费 */
    MODEL_USAGE("MODEL_USAGE", "模型调用计费"),

    /** Agent创建计费 - 已预留配置：按次收费10.0元，service_id='agent_creation' */
    AGENT_CREATION("AGENT_CREATION", "Agent创建计费"),

    /** Agent使用计费 */
    AGENT_USAGE("AGENT_USAGE", "Agent使用计费"),

    /** API调用计费 */
    API_CALL("API_CALL", "API调用计费"),

    /** 存储使用计费 */
    STORAGE_USAGE("STORAGE_USAGE", "存储使用计费");

    private final String code;
    private final String description;

    BillingType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据代码获取枚举 */
    public static BillingType fromCode(String code) {
        for (BillingType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的计费类型: " + code);
    }
}