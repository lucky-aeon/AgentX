package org.xhy.domain.llm.model.enums;

/**
 * 模型类型枚举
 */
public enum ModelType {
    
    CHAT("CHAT", "对话模型"),
    EMBEDDING("EMBEDDING", "嵌入模型");
    
    private final String code;
    private final String description;
    
    ModelType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ModelType fromCode(String code) {
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model type code: " + code);
    }
} 