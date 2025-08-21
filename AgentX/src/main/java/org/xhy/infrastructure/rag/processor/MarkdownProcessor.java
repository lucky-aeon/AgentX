package org.xhy.infrastructure.rag.processor;

import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.strategy.context.ProcessingContext;

import java.util.List;

/** 统一的Markdown处理器接口
 * 
 * 设计原则： - 提供统一的Markdown处理接口 - 支持纯净解析和RAG增强两种实现模式 - 通过依赖注入选择具体实现，无需复杂配置
 * 
 * @author claude */
public interface MarkdownProcessor {

    /** 将Markdown文本处理为段落列表
     * 
     * @param markdown Markdown文本内容
     * @param context 处理上下文，包含用户配置和LLM设置
     * @return 处理后的段落列表 */
    List<ProcessedSegment> processToSegments(String markdown, ProcessingContext context);

}