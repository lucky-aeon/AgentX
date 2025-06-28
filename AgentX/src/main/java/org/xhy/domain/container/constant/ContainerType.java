package org.xhy.domain.container.constant;

/** 容器类型枚举 */
public enum ContainerType {
    /** 用户容器 */
    USER(1, "用户容器"),
    /** 审核容器 */
    REVIEW(2, "审核容器");

    private final Integer code;
    private final String description;

    ContainerType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ContainerType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ContainerType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown container type code: " + code);
    }
}