package org.xhy.domain.llm.model.enums;

/** 服务商类型枚举 */
public enum ProviderType {
    /** 所有服务商(包括官方和用户自定义) */
    ALL("all"),

    /** 官方服务商 */
    OFFICIAL("official"),

    /** 用户自定义服务商 */
    CUSTOM("custom");

    private final String code;

    ProviderType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 根据code获取对应的枚举值
     * @param code 类型编码
     * @return 对应的枚举，若不存在则默认返回USER类型 */
    public static ProviderType fromCode(String code) {
        for (ProviderType type : ProviderType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return ProviderType.ALL;
    }
}