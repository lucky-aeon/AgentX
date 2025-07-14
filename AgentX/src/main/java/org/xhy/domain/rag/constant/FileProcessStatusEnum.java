package org.xhy.domain.rag.constant;

/** 文件处理状态枚举
 * @author zang
 * @date 2025-01-14 */
public enum FileProcessStatusEnum {
    
    /** 初始化状态 */
    INIT_WAIT(0, "待初始化"),
    INIT_PROCESSING(1, "初始化中"),
    INIT_COMPLETED(2, "初始化完成"),
    INIT_FAILED(3, "初始化失败"),
    
    /** 向量化状态 */
    EMBEDDING_WAIT(0, "待向量化"),
    EMBEDDING_PROCESSING(1, "向量化中"),
    EMBEDDING_COMPLETED(2, "向量化完成"),
    EMBEDDING_FAILED(3, "向量化失败");
    
    private final Integer code;
    private final String description;
    
    FileProcessStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /** 根据代码获取初始化状态描述
     * @param code 状态代码
     * @return 状态描述 */
    public static String getInitStatusDescription(Integer code) {
        if (code == null) {
            return INIT_WAIT.getDescription();
        }
        return switch (code) {
            case 0 -> INIT_WAIT.getDescription();
            case 1 -> INIT_PROCESSING.getDescription();
            case 2 -> INIT_COMPLETED.getDescription();
            case 3 -> INIT_FAILED.getDescription();
            default -> "未知状态";
        };
    }
    
    /** 根据代码获取向量化状态描述
     * @param code 状态代码
     * @return 状态描述 */
    public static String getEmbeddingStatusDescription(Integer code) {
        if (code == null) {
            return EMBEDDING_WAIT.getDescription();
        }
        return switch (code) {
            case 0 -> EMBEDDING_WAIT.getDescription();
            case 1 -> EMBEDDING_PROCESSING.getDescription();
            case 2 -> EMBEDDING_COMPLETED.getDescription();
            case 3 -> EMBEDDING_FAILED.getDescription();
            default -> "未知状态";
        };
    }
}