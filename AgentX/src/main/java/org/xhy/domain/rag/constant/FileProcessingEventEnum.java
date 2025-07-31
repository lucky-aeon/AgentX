package org.xhy.domain.rag.constant;

/** 文件处理事件枚举
 * 
 * @author zang
 * @date 2025-07-17 */
public enum FileProcessingEventEnum {

    /** 开始OCR处理事件 */
    START_OCR_PROCESSING,

    /** OCR处理完成事件 */
    COMPLETE_OCR_PROCESSING,

    /** OCR处理失败事件 */
    FAIL_OCR_PROCESSING,

    /** 开始向量化处理事件 */
    START_EMBEDDING_PROCESSING,

    /** 向量化处理完成事件 */
    COMPLETE_EMBEDDING_PROCESSING,

    /** 向量化处理失败事件 */
    FAIL_EMBEDDING_PROCESSING,

    /** 重置处理状态事件 */
    RESET_PROCESSING,

    /** 更新OCR进度事件 */
    UPDATE_OCR_PROGRESS,

    /** 更新向量化进度事件 */
    UPDATE_EMBEDDING_PROGRESS
}