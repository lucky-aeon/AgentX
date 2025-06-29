package org.xhy.domain.container.constant;

/** 容器类型枚举 */
public enum ContainerType {
    /** 用户容器 */
    USER,
    /** 审核容器 */
    REVIEW;

    public static ContainerType fromCode(String code) {
        for (ContainerType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown container type code: " + code);
    }
}