package org.xhy.domain.file.constant;

/** 文件类型枚举
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
public enum FileTypeEnum {

    /** RAG 文档文件 */
    RAG("rag", "RAG文档"),

    /** 用户头像文件 */
    AVATAR("avatar", "用户头像"),

    /** 通用文件（默认） */
    GENERAL("general", "通用文件");

    private final String code;
    private final String description;

    FileTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据代码获取文件类型
     * 
     * @param code 类型代码
     * @return 文件类型枚举 */
    public static FileTypeEnum fromCode(String code) {
        if (code == null) {
            return GENERAL;
        }

        for (FileTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        return GENERAL;
    }
}