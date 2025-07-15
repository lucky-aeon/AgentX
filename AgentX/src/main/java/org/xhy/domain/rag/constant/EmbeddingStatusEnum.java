package org.xhy.domain.rag.constant;

/**
 * 向量化状态枚举
 * @author zang
 * @date 2025-01-15
 */
public enum EmbeddingStatusEnum {
    
    /** 未初始化 */
    UNINITIALIZED(0, "未初始化"),
    
    /** 初始化中 */
    INITIALIZING(1, "初始化中"),
    
    /** 已初始化 */
    INITIALIZED(2, "已初始化"),
    
    /** 初始化失败 */
    INITIALIZATION_FAILED(3, "初始化失败");
    
    private final Integer code;
    private final String description;
    
    EmbeddingStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return 枚举值
     */
    public static EmbeddingStatusEnum fromCode(Integer code) {
        if (code == null) {
            return UNINITIALIZED;
        }
        for (EmbeddingStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return UNINITIALIZED;
    }
}