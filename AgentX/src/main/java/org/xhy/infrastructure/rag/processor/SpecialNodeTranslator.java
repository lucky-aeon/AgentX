package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.enhancer.SegmentEnhancer;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.SpecialNode;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 特殊节点翻译器
 * 
 * 使用 Flexmark AST 处理 markdown 中的特殊节点翻译：
 * - 代码块 -> 自然语言描述
 * - 表格 -> 结构化文本描述
 * - 图片 -> OCR文本识别
 * - 公式 -> 数学表达式描述
 * 
 * 设计原则：
 * - 单次 AST 遍历完成所有特殊节点的识别和翻译
 * - 不使用正则表达式，完全基于 AST 结构分析
 * - 保持与 PureMarkdownProcessor 的架构一致性
 * 
 * @author claude
 */
@Component
public class SpecialNodeTranslator {
    
    private static final Logger log = LoggerFactory.getLogger(SpecialNodeTranslator.class);
    
    private final List<SegmentEnhancer> enhancers;
    private final Parser parser;
    
    @Autowired
    public SpecialNodeTranslator(List<SegmentEnhancer> enhancers) {
        this.enhancers = enhancers;
        
        // 配置与 PureMarkdownProcessor 一致的 Flexmark 解析器
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
        
        log.info("SpecialNodeTranslator initialized with {} enhancers", enhancers.size());
    }
    
    /**
     * 翻译内容中的特殊节点
     * 
     * 使用 AST 单次遍历完成所有特殊节点的识别和翻译
     * 
     * @param originalContent 原始内容
     * @param context 处理上下文
     * @return 翻译后的内容
     */
    public String translateSpecialNodes(String originalContent, ProcessingContext context) {
        if (originalContent == null || originalContent.trim().isEmpty()) {
            return originalContent;
        }
        
        try {
            // 解析 markdown 为 AST
            Node document = parser.parse(originalContent);
            
            // 单次遍历处理所有特殊节点
            String translatedContent = processDocumentWithTranslation(document, originalContent, context);
            
            if (translatedContent.equals(originalContent)) {
                log.debug("No special nodes found or translated, returning original content");
            } else {
                log.debug("Special node translation completed. Original length: {}, Translated length: {}", 
                        originalContent.length(), translatedContent.length());
            }
            
            return translatedContent;
            
        } catch (Exception e) {
            log.error("Error translating special nodes: {}", e.getMessage(), e);
            return originalContent; // 出错时返回原文
        }
    }
    
    /**
     * 处理整个文档，翻译所有特殊节点
     * 
     * @param document AST 文档根节点
     * @param originalContent 原始内容文本
     * @param context 处理上下文
     * @return 翻译后的内容
     */
    private String processDocumentWithTranslation(Node document, String originalContent, ProcessingContext context) {
        // 收集所有需要翻译的特殊节点及其位置信息
        List<NodeTranslation> translations = new java.util.ArrayList<>();
        collectSpecialNodesForTranslation(document, translations, context);
        
        // 如果没有找到特殊节点，直接返回原内容
        if (translations.isEmpty()) {
            return originalContent;
        }
        
        // 按节点在文档中的位置从后往前排序，避免替换时位置偏移
        translations.sort((a, b) -> Integer.compare(b.startOffset, a.startOffset));
        
        // 应用所有翻译替换
        StringBuilder result = new StringBuilder(originalContent);
        for (NodeTranslation translation : translations) {
            result.replace(translation.startOffset, translation.endOffset, translation.translatedContent);
        }
        
        log.debug("Applied {} node translations", translations.size());
        return result.toString();
    }
    
    /**
     * 收集文档中所有需要翻译的特殊节点
     * 
     * @param node 当前遍历的节点
     * @param translations 收集翻译信息的列表
     * @param context 处理上下文
     */
    private void collectSpecialNodesForTranslation(Node node, List<NodeTranslation> translations, ProcessingContext context) {
        // 检查当前节点是否为特殊节点
        if (isSpecialNode(node)) {
            String translatedContent = translateSpecialNode(node, context);
            if (translatedContent != null && !translatedContent.equals(node.getChars().toString())) {
                NodeTranslation translation = new NodeTranslation(
                    node.getStartOffset(),
                    node.getEndOffset(), 
                    translatedContent
                );
                translations.add(translation);
                log.debug("Collected translation for {} node at offset {}-{}", 
                        getNodeType(node), node.getStartOffset(), node.getEndOffset());
            }
        }
        
        // 递归处理子节点
        for (Node child : node.getChildren()) {
            collectSpecialNodesForTranslation(child, translations, context);
        }
    }
    
    /**
     * 判断节点是否为特殊节点
     * 
     * @param node AST 节点
     * @return 是否为特殊节点
     */
    private boolean isSpecialNode(Node node) {
        return node instanceof FencedCodeBlock ||
               node instanceof IndentedCodeBlock ||
               node instanceof TableBlock ||
               node instanceof Image ||
               node instanceof Code ||
               isFormulaNode(node);
    }
    
    /**
     * 检查是否为公式节点（简单的文本模式识别）
     * 
     * @param node AST 节点
     * @return 是否为公式节点
     */
    private boolean isFormulaNode(Node node) {
        if (node instanceof Text) {
            String text = node.getChars().toString();
            return text.matches(".*\\$\\$[^$]*\\$\\$.*") || text.matches(".*\\$[^$]*\\$.*");
        }
        return false;
    }
    
    /**
     * 获取节点类型
     * 
     * @param node AST 节点
     * @return 节点类型
     */
    private SegmentType getNodeType(Node node) {
        if (node instanceof Image) {
            return SegmentType.IMAGE;
        } else if (node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
            return SegmentType.CODE;
        } else if (node instanceof com.vladsch.flexmark.ext.tables.TableBlock) {
            return SegmentType.TABLE;
        } else if (node instanceof Code) {
            return SegmentType.CODE;
        } else if (isFormulaNode(node)) {
            return SegmentType.FORMULA;
        } else {
            return SegmentType.RAW;
        }
    }
    
    /**
     * 翻译特殊节点
     * 
     * @param node 特殊节点
     * @param context 处理上下文
     * @return 翻译后的内容，如果无法翻译则返回 null
     */
    private String translateSpecialNode(Node node, ProcessingContext context) {
        try {
            SegmentType nodeType = getNodeType(node);
            String originalContent = node.getChars().toString();
            
            // 创建 ProcessedSegment 用于增强器处理
            Map<String, Object> metadata = extractNodeMetadata(node);
            ProcessedSegment segment = new ProcessedSegment(originalContent, nodeType, metadata);
            
            // 尝试使用增强器处理
            for (SegmentEnhancer enhancer : enhancers) {
                if (enhancer.canEnhance(segment)) {
                    ProcessedSegment enhanced = enhancer.enhance(segment, context);
                    if (enhanced != null && !enhanced.getContent().equals(originalContent)) {
                        log.debug("{} node translated by {}: {} chars -> {} chars", 
                                nodeType, enhancer.getClass().getSimpleName(), 
                                originalContent.length(), enhanced.getContent().length());
                        return enhanced.getContent();
                    }
                }
            }
            
            // 如果没有合适的增强器，使用基础翻译
            return translateNodeBasic(node, nodeType);
            
        } catch (Exception e) {
            log.warn("Failed to translate {} node, using fallback: {}", getNodeType(node), e.getMessage());
            return translateNodeBasic(node, getNodeType(node));
        }
    }
    
    /**
     * 提取节点元数据
     * 
     * @param node AST 节点
     * @return 节点元数据
     */
    private Map<String, Object> extractNodeMetadata(Node node) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (node instanceof Image) {
            Image image = (Image) node;
            metadata.put("url", image.getUrl().toString());
            metadata.put("alt", extractTextContent(image));
        } else if (node instanceof FencedCodeBlock) {
            FencedCodeBlock code = (FencedCodeBlock) node;
            if (code.getInfo() != null && !code.getInfo().isBlank()) {
                metadata.put("language", code.getInfo().toString().trim());
            }
            metadata.put("lines", code.getContentChars().toString().split("\n").length);
        } else if (node instanceof IndentedCodeBlock) {
            IndentedCodeBlock code = (IndentedCodeBlock) node;
            metadata.put("language", "text");
            metadata.put("lines", code.getContentChars().toString().split("\n").length);
        } else if (node instanceof Code) {
            metadata.put("language", "inline");
        }
        
        return metadata;
    }
    
    /**
     * 基础节点翻译（回退方案）
     * 
     * @param node AST 节点
     * @param nodeType 节点类型
     * @return 翻译后的内容
     */
    private String translateNodeBasic(Node node, SegmentType nodeType) {
        switch (nodeType) {
            case IMAGE:
                return translateImageBasic((Image) node);
            case CODE:
                if (node instanceof Code) {
                    return translateInlineCodeBasic((Code) node);
                } else {
                    return translateCodeBlockBasic(node);
                }
            case TABLE:
                return translateTableBasic((com.vladsch.flexmark.ext.tables.TableBlock) node);
            case FORMULA:
                return translateFormulaBasic(node);
            default:
                return node.getChars().toString();
        }
    }
    
    /**
     * 基础图片翻译（回退方案）
     * 
     * @param image 图片节点
     * @return 翻译后的内容
     */
    private String translateImageBasic(Image image) {
        String url = image.getUrl().toString();
        String alt = extractTextContent(image);
        
        if (alt != null && !alt.trim().isEmpty()) {
            return String.format("这是一张图片：%s（图片地址：%s）", alt.trim(), url);
        } else {
            return String.format("这是一张图片（图片地址：%s）", url);
        }
    }
    
    /**
     * 基础代码块翻译（回退方案）
     * 
     * @param codeNode 代码节点
     * @return 翻译后的内容
     */
    private String translateCodeBlockBasic(Node codeNode) {
        String codeContent;
        String language = null;
        
        if (codeNode instanceof FencedCodeBlock) {
            FencedCodeBlock codeBlock = (FencedCodeBlock) codeNode;
            codeContent = codeBlock.getContentChars().toString().trim();
            if (codeBlock.getInfo() != null && !codeBlock.getInfo().isBlank()) {
                language = codeBlock.getInfo().toString().trim();
            }
        } else if (codeNode instanceof IndentedCodeBlock) {
            IndentedCodeBlock codeBlock = (IndentedCodeBlock) codeNode;
            codeContent = codeBlock.getContentChars().toString().trim();
            language = "text";
        } else {
            codeContent = codeNode.getChars().toString().trim();
            language = "unknown";
        }
        
        if (language == null || language.isEmpty()) {
            return String.format("这是一段代码示例：%s", codeContent);
        } else {
            return String.format("这是一段%s代码示例：%s", language, codeContent);
        }
    }
    
    /**
     * 基础内联代码翻译（回退方案）
     * 
     * @param code 内联代码节点
     * @return 翻译后的内容
     */
    private String translateInlineCodeBasic(Code code) {
        String codeText = code.getChars().toString();
        String content = codeText.replaceAll("`", "").trim();
        
        if (content.isEmpty()) {
            return "代码";
        }
        
        return String.format("代码 %s", content);
    }
    
    /**
     * 基础表格翻译（回退方案）
     * 
     * @param table 表格节点
     * @return 翻译后的内容
     */
    private String translateTableBasic(com.vladsch.flexmark.ext.tables.TableBlock table) {
        String tableContent = table.getChars().toString();
        String[] lines = tableContent.split("\n");
        
        if (lines.length < 2) {
            return "这是一个表格：" + tableContent;
        }
        
        // 提取表头
        String headerLine = lines[0];
        String[] headers = headerLine.split("\\|");
        StringBuilder description = new StringBuilder("这是一个包含以下列的表格：");
        
        for (String header : headers) {
            String trimmed = header.trim();
            if (!trimmed.isEmpty()) {
                description.append(trimmed).append("、");
            }
        }
        
        if (description.toString().endsWith("、")) {
            description.setLength(description.length() - 1);
        }
        
        description.append("。表格包含").append(Math.max(0, lines.length - 2)).append("行数据。");
        
        return description.toString();
    }
    
    /**
     * 基础公式翻译（回退方案）
     * 
     * @param node 公式节点
     * @return 翻译后的内容
     */
    private String translateFormulaBasic(Node node) {
        String formula = node.getChars().toString();
        String cleanFormula = formula.replaceAll("\\$+", "").trim();
        
        if (cleanFormula.isEmpty()) {
            return "这是一个数学公式";
        }
        
        return String.format("这是一个数学公式：%s", cleanFormula);
    }
    
    /**
     * 提取节点的纯文本内容
     * 
     * @param node AST 节点
     * @return 纯文本内容
     */
    private String extractTextContent(Node node) {
        if (node == null) {
            return "";
        }
        
        StringBuilder text = new StringBuilder();
        extractTextRecursively(node, text);
        return text.toString();
    }
    
    /**
     * 递归提取文本内容
     * 
     * @param node 当前节点
     * @param text 文本构建器
     */
    private void extractTextRecursively(Node node, StringBuilder text) {
        if (node instanceof Text) {
            text.append(node.getChars().toString());
        } else {
            // 递归处理子节点
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
        }
    }
    
    /**
     * 节点翻译信息类
     */
    private static class NodeTranslation {
        final int startOffset;
        final int endOffset;
        final String translatedContent;
        
        NodeTranslation(int startOffset, int endOffset, String translatedContent) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.translatedContent = translatedContent;
        }
    }
}