package org.xhy.infrastructure.rag.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.enhancer.SegmentEnhancer;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** RAG增强Markdown处理器
 * 
 * 设计模式：装饰器模式
 * - 包装纯净解析器，提供基础的结构化分段功能
 * - 使用增强器列表，对每个段落进行RAG相关的增强处理
 * - 保持接口一致性，调用方无需感知内部实现差异
 * 
 * 处理流程：
 * 1. 委托纯净解析器进行基础Markdown分段
 * 2. 对每个段落按优先级应用相关增强器
 * 3. 返回增强后的段落列表
 * 
 * 适用场景：
 * - RAG文档处理：需要LLM增强的生产环境
 * - 智能分析：需要将特殊内容转换为搜索友好的文本
 * 
 * @author claude */
@Component("ragEnhancedMarkdownProcessor")
public class RagEnhancedMarkdownProcessor implements MarkdownProcessor {

    private static final Logger log = LoggerFactory.getLogger(RagEnhancedMarkdownProcessor.class);

    private final MarkdownProcessor pureProcessor;
    private final List<SegmentEnhancer> enhancers;

    @Autowired
    public RagEnhancedMarkdownProcessor(PureMarkdownProcessor pureProcessor,
                                       List<SegmentEnhancer> enhancers) {
        this.pureProcessor = pureProcessor;
        this.enhancers = enhancers;
        
        // 按优先级排序增强器
        this.enhancers.sort(Comparator.comparingInt(SegmentEnhancer::getPriority));
        
        log.info("RAG Enhanced Markdown Processor initialized with {} enhancers: {}", 
                 enhancers.size(), 
                 enhancers.stream().map(e -> e.getClass().getSimpleName()).collect(Collectors.toList()));
    }

    @Override
    public List<ProcessedSegment> processToSegments(String markdown, ProcessingContext context) {
        if (markdown == null || markdown.trim().isEmpty()) {
            log.debug("Empty markdown content, returning empty list");
            return List.of();
        }

        try {
            log.debug("Starting RAG enhanced processing for markdown content (length: {})", markdown.length());
            
            // 第1步：委托纯净解析器进行基础分段
            List<ProcessedSegment> baseSegments = pureProcessor.processToSegments(markdown, context);
            log.debug("Pure processor generated {} base segments", baseSegments.size());

            // 第2步：对每个段落应用增强器
            List<ProcessedSegment> enhancedSegments = baseSegments.stream()
                    .map(segment -> enhanceSegment(segment, context))
                    .collect(Collectors.toList());

            log.info("RAG enhanced processing completed: {} base segments -> {} enhanced segments", 
                     baseSegments.size(), enhancedSegments.size());
            
            return enhancedSegments;

        } catch (Exception e) {
            log.error("Failed to process markdown with RAG enhancement", e);
            
            // 降级处理：如果增强失败，至少返回纯净解析的结果
            try {
                log.warn("Falling back to pure processing due to enhancement failure");
                return pureProcessor.processToSegments(markdown, context);
            } catch (Exception fallbackError) {
                log.error("Pure processor fallback also failed", fallbackError);
                
                // 最终回退：返回整个文档作为单个段落
                ProcessedSegment fallback = new ProcessedSegment(markdown, "text", null);
                fallback.setOrder(0);
                return List.of(fallback);
            }
        }
    }

    /** 增强单个段落
     * 
     * 应用所有能够处理该段落类型的增强器，按优先级顺序处理
     * 
     * @param segment 原始段落
     * @param context 处理上下文
     * @return 增强后的段落 */
    private ProcessedSegment enhanceSegment(ProcessedSegment segment, ProcessingContext context) {
        ProcessedSegment currentSegment = segment;
        int enhancedCount = 0;

        try {
            // 依次应用所有匹配的增强器
            for (SegmentEnhancer enhancer : enhancers) {
                try {
                    if (enhancer.canEnhance(currentSegment)) {
                        log.debug("Applying {} to segment (type: {}, order: {})", 
                                 enhancer.getType(), currentSegment.getType(), currentSegment.getOrder());
                        
                        ProcessedSegment enhanced = enhancer.enhance(currentSegment, context);
                        if (enhanced != null && !enhanced.equals(currentSegment)) {
                            currentSegment = enhanced;
                            enhancedCount++;
                            
                            log.debug("Successfully enhanced segment with {}", enhancer.getType());
                        } else {
                            log.debug("Enhancer {} returned unchanged segment", enhancer.getType());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Enhancer {} failed to process segment (type: {}): {}", 
                            enhancer.getType(), currentSegment.getType(), e.getMessage());
                    // 继续处理其他增强器，不因单个增强器失败而中断
                }
            }

            if (enhancedCount > 0) {
                log.debug("Segment enhanced by {} enhancers: type={}, original_length={}, final_length={}", 
                         enhancedCount, segment.getType(), 
                         segment.getContent().length(), currentSegment.getContent().length());
            }

        } catch (Exception e) {
            log.error("Unexpected error during segment enhancement", e);
            // 返回原始段落
            return segment;
        }

        return currentSegment;
    }

    /** 获取配置的增强器列表（用于调试和监控） */
    public List<SegmentEnhancer> getConfiguredEnhancers() {
        return List.copyOf(enhancers);
    }

    /** 获取纯净处理器引用（用于测试） */
    public MarkdownProcessor getPureProcessor() {
        return pureProcessor;
    }
}