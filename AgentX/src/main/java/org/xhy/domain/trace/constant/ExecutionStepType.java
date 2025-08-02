package org.xhy.domain.trace.constant;

/** 执行步骤类型枚举 */
public enum ExecutionStepType {

    /** 用户消息 */
    USER_MESSAGE("USER_MESSAGE", "用户消息"),

    /** AI响应 */
    AI_RESPONSE("AI_RESPONSE", "AI响应"),

    /** 工具调用 */
    TOOL_CALL("TOOL_CALL", "工具调用");

    private final String code;
    private final String description;

    ExecutionStepType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据代码获取枚举值 */
    public static ExecutionStepType fromCode(String code) {
        for (ExecutionStepType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的执行步骤类型: " + code);
    }
}