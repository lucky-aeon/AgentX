package org.xhy.domain.rag.constant;

/** 检索类型枚举 定义混合检索系统中的不同检索方式
 * 
 * @author claude */
public enum SearchType {

    /** 向量检索 */
    VECTOR("vector", "向量检索"),

    /** 关键词检索 */
    KEYWORD("keyword", "关键词检索"),

    /** 混合检索 */
    HYBRID("hybrid", "混合检索");

    private final String code;
    private final String description;

    SearchType(String code, String description) {
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
     * @param code 检索类型代码
     * @return 对应的枚举，未找到则返回null */
    public static SearchType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (SearchType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return code;
    }
}