package org.xhy.domain.container.constant;

/** 容器状态枚举 */
public enum ContainerStatus {
    /** 创建中 */
    CREATING(1, "创建中"),
    /** 运行中 */
    RUNNING(2, "运行中"),
    /** 已停止 */
    STOPPED(3, "已停止"),
    /** 错误状态 */
    ERROR(4, "错误状态"),
    /** 删除中 */
    DELETING(5, "删除中"),
    /** 已删除 */
    DELETED(6, "已删除"),
    /** 已暂停 */
    SUSPENDED(7, "已暂停");

    private final Integer code;
    private final String description;

    ContainerStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ContainerStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ContainerStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown container status code: " + code);
    }
}