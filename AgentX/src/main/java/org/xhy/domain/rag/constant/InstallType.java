package org.xhy.domain.rag.constant;

/** RAG安装类型枚举
 * @author xhy
 * @date 2025-07-19 <br/>
 */
public enum InstallType {

    /** 引用类型 - 动态引用原始数据集，支持实时更新 */
    REFERENCE("REFERENCE", "引用类型"),

    /** 快照类型 - 使用版本快照数据，内容固定不变 */
    SNAPSHOT("SNAPSHOT", "快照类型");

    private final String code;
    private final String description;

    InstallType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据代码获取枚举
     * 
     * @param code 代码
     * @return 枚举值
     * @throws IllegalArgumentException 如果代码无效 */
    public static InstallType fromCode(String code) {
        for (InstallType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的安装类型代码: " + code);
    }

    /** 根据代码获取枚举，支持默认值
     * 
     * @param code 代码
     * @param defaultType 默认类型
     * @return 枚举值 */
    public static InstallType fromCodeOrDefault(String code, InstallType defaultType) {
        try {
            return fromCode(code);
        } catch (IllegalArgumentException e) {
            return defaultType;
        }
    }

    /** 检查是否为引用类型
     * 
     * @return 是否为引用类型 */
    public boolean isReference() {
        return this == REFERENCE;
    }

    /** 检查是否为快照类型
     * 
     * @return 是否为快照类型 */
    public boolean isSnapshot() {
        return this == SNAPSHOT;
    }
}