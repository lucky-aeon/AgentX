package org.xhy.domain.rag.enhancer;

import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

/** 段落增强器接口
 * 
 * 设计原则：
 * - 单一职责：每个增强器专注处理特定类型的段落
 * - 职责分离：只做增强处理，不做基础解析
 * - 可选增强：可以判断是否需要处理某个段落
 * - 链式处理：支持多个增强器依次处理同一段落
 * 
 * @author claude */
public interface SegmentEnhancer {

    /** 判断是否可以增强该段落
     * 
     * @param segment 待处理的段落
     * @return 是否可以处理该段落 */
    boolean canEnhance(ProcessedSegment segment);

    /** 增强处理段落
     * 
     * 对段落进行RAG相关的增强处理，如：
     * - 代码块转换为自然语言描述
     * - 表格内容智能分析
     * - 图片OCR识别
     * - 公式转换为文本描述
     * - 内容翻译等
     * 
     * @param segment 原始段落
     * @param context 处理上下文，包含LLM配置等
     * @return 增强后的段落 */
    ProcessedSegment enhance(ProcessedSegment segment, ProcessingContext context);

    /** 获取增强器优先级
     * 
     * 数字越小优先级越高，用于确定多个增强器的处理顺序
     * 
     * @return 优先级数值 */
    default int getPriority() {
        return 100;
    }

    /** 获取增强器类型标识
     * 
     * 用于日志记录和调试
     * 
     * @return 类型标识 */
    default String getType() {
        return this.getClass().getSimpleName().replace("Enhancer", "").toLowerCase();
    }
}