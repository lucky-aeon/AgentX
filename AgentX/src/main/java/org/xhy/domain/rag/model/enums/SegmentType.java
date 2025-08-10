package org.xhy.domain.rag.model.enums;

/** 段落类型枚举
 *
 * 定义了Markdown处理后的段落类型，避免魔法字符串的使用
 *
 * @author claude */
public enum SegmentType {

    /** 普通文本段落 */
    TEXT("text"),

    /** 语义段落（按标题分组的内容） */
    SECTION("section"),

    /** 表格内容 */
    TABLE("table"),

    /** 数学公式 */
    FORMULA("formula"),

    /** 图片内容 */
    IMAGE("image"),

    /** 代码块 */
    CODE("code"),

    /** 引用块 */
    BLOCKQUOTE("blockquote"),

    /** 混合内容（包含多种特殊节点的段落） */
    MIXED("mixed"),

    /** 原始内容（处理失败时的回退） */
    RAW("raw");

    private final String value;

    SegmentType(String value) {
        this.value = value;
    }

    /** 获取段落类型的字符串值 */
    public String getValue() {
        return value;
    }

    /** 从字符串值创建枚举实例 */
    public static SegmentType fromValue(String value) {
        for (SegmentType type : SegmentType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown segment type: " + value);
    }

    /** 检查是否为特殊内容类型 */
    public boolean isSpecialContent() {
        return this == IMAGE || this == CODE || this == TABLE || this == FORMULA;
    }

    /** 检查是否为容器类型（可以包含其他内容） */
    public boolean isContainer() {
        return this == SECTION || this == MIXED;
    }

    @Override
    public String toString() {
        return value;
    }
}