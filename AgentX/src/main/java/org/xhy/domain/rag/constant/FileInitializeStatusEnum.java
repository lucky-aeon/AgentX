package org.xhy.domain.rag.constant;

/** 文件初始化状态枚举
 * @author zang
 * @date 2025-01-15 */
public enum FileInitializeStatusEnum {

    /** 待初始化 */
    INITIALIZE_WAIT(0, "待初始化"),

    /** 初始化中 */
    INITIALIZING(1, "初始化中"),

    /** 已初始化 */
    INITIALIZED(2, "已初始化"),

    /** 初始化失败 */
    INITIALIZATION_FAILED(3, "初始化失败");

    private final Integer code;
    private final String description;

    FileInitializeStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据状态码获取枚举
     * @param code 状态码
     * @return 枚举值 */
    public static FileInitializeStatusEnum fromCode(Integer code) {
        if (code == null) {
            return INITIALIZE_WAIT;
        }
        for (FileInitializeStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return INITIALIZE_WAIT;
    }
}