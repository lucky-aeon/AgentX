package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.domain.rag.straegy.MarkdownTokenProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.Comparator;
import java.util.List;

/** 增强的Markdown处理器
 * 
 * @author claude */
@Component
public class EnhancedMarkdownProcessor {

    private static final Logger log = LoggerFactory.getLogger(EnhancedMarkdownProcessor.class);

    private final MarkdownProcessor pureProcessor;
    private final List<MarkdownTokenProcessor> processors;

    public EnhancedMarkdownProcessor(List<MarkdownTokenProcessor> processors,
                                     @Qualifier("ragEnhancedMarkdownProcessor") MarkdownProcessor pureProcessor) {
        this.processors = processors;
        this.pureProcessor = pureProcessor;
        // 按优先级排序处理器
        this.processors.sort(Comparator.comparingInt(MarkdownTokenProcessor::getPriority));

        log.info("EnhancedMarkdownProcessor initialized with {} processors", processors.size());
    }

    /** 处理Markdown文本，生成处理后的段落列表
     * 
     * 🔄 迁移说明：现在直接委托给纯净处理器，保持向后兼容
     *
     * @param markdown Markdown文本
     * @param context 处理上下文
     * @return 处理后的段落列表 */
    public List<ProcessedSegment> processToSegments(String markdown, ProcessingContext context) {
        log.info("EnhancedMarkdownProcessor delegating to pure processor for compatibility");
        return pureProcessor.processToSegments(markdown, context);
    }


}