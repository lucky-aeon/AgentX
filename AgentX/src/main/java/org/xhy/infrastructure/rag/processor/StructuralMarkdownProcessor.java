package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.strategy.context.ProcessingContext;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.util.ArrayList;
import java.util.List;

/** 结构化Markdown处理器
 * 
 * 设计职责： - 协调各个组件完成Markdown结构化解析和语义分段 - 不进行任何LLM调用或内容增强 - 按语义结构（标题层级）进行合理分段 - 支持纯原文拆分模式，用于二次分割架构
 * 
 * 适用场景： - 单元测试（无需mock外部服务） - 基础文档解析 - 性能要求高的场景 - 二次分割架构的第一阶段处理 */
@Component("structuralMarkdownProcessor")
public class StructuralMarkdownProcessor implements MarkdownProcessor {

    private static final Logger log = LoggerFactory.getLogger(StructuralMarkdownProcessor.class);

    private final MarkdownAstParser astParser;
    private final MarkdownTextExtractor textExtractor;
    private final DocumentTreeBuilder treeBuilder;;
    private MarkdownProcessorProperties markdownProperties;

    // 纯原文拆分模式标志
    private boolean rawMode = false;

    public StructuralMarkdownProcessor(MarkdownAstParser astParser, MarkdownTextExtractor textExtractor,
            DocumentTreeBuilder treeBuilder, MarkdownProcessorProperties markdownProperties) {
        this.astParser = astParser;
        this.textExtractor = textExtractor;
        this.treeBuilder = treeBuilder;
        this.markdownProperties = markdownProperties;
    }

    /** 默认构造函数，使用层次化分割的推荐配置 */
    public StructuralMarkdownProcessor() {
        // 使用层次化分割的默认配置
        this.markdownProperties = new MarkdownProcessorProperties();
        MarkdownProcessorProperties.SegmentSplit segmentSplit = new MarkdownProcessorProperties.SegmentSplit();
        segmentSplit.setEnabled(true); // 启用层次化分割
        segmentSplit.setMaxLength(1800); // 默认最大长度
        segmentSplit.setMinLength(200); // 默认最小长度
        segmentSplit.setBufferSize(100); // 默认缓冲区
        this.markdownProperties.setSegmentSplit(segmentSplit);

        // 创建默认组件
        this.astParser = new MarkdownAstParser();
        this.textExtractor = new MarkdownTextExtractor();
        this.treeBuilder = new DocumentTreeBuilder(textExtractor);
    }

    @Override
    public List<ProcessedSegment> processToSegments(String markdown, ProcessingContext context) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            if (rawMode) {
                return processRawSegments(markdown, context);
            } else {
                return processRegularSegments(markdown, context);
            }

        } catch (Exception e) {
            log.error("Failed to process markdown with structural processor", e);
            // 回退方案：整个文档作为一个段落
            ProcessedSegment fallback = new ProcessedSegment(markdown, SegmentType.TEXT, null);
            fallback.setOrder(0);
            return List.of(fallback);
        }
    }

    /** 纯原文拆分模式 - 保持原始格式，不进行任何特殊处理 */
    private List<ProcessedSegment> processRawSegments(String markdown, ProcessingContext context) {
        log.debug("Processing markdown in raw mode (preserving original content)");

        // 解析Markdown为AST
        Node document = astParser.parse(markdown);

        // 构建保持原始内容的文档树
        DocumentTree documentTree = treeBuilder.buildRawDocumentTree(document, markdownProperties.getSegmentSplit());

        // 执行基于真实内容长度的分割
        List<ProcessedSegment> segments = documentTree.performHierarchicalSplit();

        // 设置段落顺序
        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).setOrder(i);
        }

        log.info("Raw processing completed: {} segments generated", segments.size());
        return segments;
    }

    /** 常规处理模式 - 简化的语义分段 */
    private List<ProcessedSegment> processRegularSegments(String markdown, ProcessingContext context) {
        // 解析Markdown为AST
        Node document = astParser.parse(markdown);

        List<ProcessedSegment> segments = new ArrayList<>();
        int order = 0;

        // 使用语义感知遍历进行分段
        order = processSemanticStructure(document, segments, order);

        log.info("Regular processing completed: {} segments generated", segments.size());
        return segments;
    }

    /** 设置原文拆分模式
     * 
     * @param rawMode true=纯原文模式，false=常规处理模式 */
    public void setRawMode(boolean rawMode) {
        this.rawMode = rawMode;
        log.debug("Raw mode set to: {}", rawMode);
    }

    /** 获取当前处理模式 */
    public boolean isRawMode() {
        return rawMode;
    }

    /** 语义结构处理 - 构建文档树并执行层次化分割 */
    private int processSemanticStructure(Node document, List<ProcessedSegment> segments, int order) {
        // 构建文档树
        DocumentTree documentTree = treeBuilder.buildDocumentTree(document, markdownProperties.getSegmentSplit());
        log.debug("Built document tree: {}", documentTree.getTreeStatistics());

        // 执行层次化分割
        List<ProcessedSegment> hierarchicalSegments = documentTree.performHierarchicalSplit();

        // 设置段落顺序并添加到结果列表
        int currentOrder = order;
        for (ProcessedSegment segment : hierarchicalSegments) {
            segment.setOrder(currentOrder++);
            segments.add(segment);
        }

        log.info("Hierarchical processing completed: {} segments generated", hierarchicalSegments.size());
        return currentOrder;
    }
}