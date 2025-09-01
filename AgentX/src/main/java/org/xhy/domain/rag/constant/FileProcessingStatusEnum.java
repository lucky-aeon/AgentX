package org.xhy.domain.rag.constant;

/** 文件处理状态枚举
 * 
 * @author zang
 * @date 2025-07-17 */
public enum FileProcessingStatusEnum {

    /** 已上传，待开始处理 */
    UPLOADED(0, "已上传"),

    /** OCR处理中 */
    OCR_PROCESSING(1, "OCR处理中"),

    /** OCR处理完成，待向量化 */
    OCR_COMPLETED(2, "OCR处理完成"),

    /** 向量化处理中 */
    EMBEDDING_PROCESSING(3, "向量化处理中"),

    /** 全部处理完成 */
    COMPLETED(4, "处理完成"),

    /** OCR处理失败 */
    OCR_FAILED(5, "OCR处理失败"),

    /** 向量化处理失败 */
    EMBEDDING_FAILED(6, "向量化处理失败");

    private final Integer code;
    private final String description;

    FileProcessingStatusEnum(Integer code, String description) {
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
     * 
     * @param code 状态码
     * @return 状态枚举 */
    public static FileProcessingStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FileProcessingStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的文件处理状态码: " + code);
    }

    /** 是否为处理中状态
     * 
     * @return 是否处理中 */
    public boolean isProcessing() {
        return this == OCR_PROCESSING || this == EMBEDDING_PROCESSING;
    }

    /** 是否为失败状态
     * 
     * @return 是否失败 */
    public boolean isFailed() {
        return this == OCR_FAILED || this == EMBEDDING_FAILED;
    }

    /** 是否为完成状态
     * 
     * @return 是否完成 */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /** 是否可以开始OCR处理
     * 
     * @return 是否可以开始OCR */
    public boolean canStartOcr() {
        return this == UPLOADED;
    }

    /** 是否可以开始向量化处理
     * 
     * @return 是否可以开始向量化 */
    public boolean canStartEmbedding() {
        return this == OCR_COMPLETED;
    }

    // 判断当前文件的流程是否处于某个流程之后
    public boolean isAfter(FileProcessingStatusEnum other) {
        return this.code >= other.code;
    }


}