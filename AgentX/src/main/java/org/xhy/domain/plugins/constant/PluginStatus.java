package org.xhy.domain.plugins.constant;

/**
 * 插件状态枚举
 */
public enum PluginStatus {
    ENABLED(1, "启用"),
    DISABLED(0, "禁用"),
    PENDING(2, "待审核");

    private final int code;
    private final String description;

    /**
     * 构造函数
     */
    PluginStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取枚举值
     */
    public static PluginStatus fromCode(int code) {
        for (PluginStatus status : PluginStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }    
}
