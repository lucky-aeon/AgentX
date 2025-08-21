package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.rag.translator.NodeTranslatorService;

/** 特殊节点翻译器
 * 
 * 使用 Flexmark AST 重写 markdown 文档，翻译特殊节点： - 代码块 -> 自然语言描述 - 表格 -> 结构化文本描述 - 图片 -> OCR文本识别 - 公式 -> 数学表达式描述
 * 
 * 设计原则： - 基于 AST 的文档重写，保持结构和位置的准确性 - 直接输出翻译后的完整文本，不使用占位符 - 集成 NodeTranslatorService 进行实际的翻译工作 - 递归处理嵌套节点，保持格式完整性
 * 
 * @author claude */
@Component
public class SpecialNodeTranslator {

    private static final Logger log = LoggerFactory.getLogger(SpecialNodeTranslator.class);

    private final NodeTranslatorService translatorService;
    private final Parser parser;

    public SpecialNodeTranslator(NodeTranslatorService translatorService) {
        this.translatorService = translatorService;

        // 配置与 PureMarkdownProcessor 一致的 Flexmark 解析器
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, java.util.List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();

        log.info("SpecialNodeTranslator initialized with translator service: {}",
                translatorService.getTranslatorInfo());
    }

    /** 翻译内容中的特殊节点
     * 
     * 使用 AST 文档重写完成所有特殊节点的翻译
     * 
     * @param originalContent 原始 markdown 内容
     * @param context 处理上下文
     * @return 翻译后的完整文本 */
    public String translateSpecialNodes(String originalContent, ProcessingContext context) {
        if (originalContent == null || originalContent.trim().isEmpty()) {
            return originalContent;
        }

        try {
            // 解析 markdown 为 AST
            Node document = parser.parse(originalContent);

            // 使用文档重写器处理所有节点
            StringBuilder result = new StringBuilder();
            rewriteDocument(document, result, context);

            String translatedContent = result.toString();

            if (translatedContent.equals(originalContent)) {
                log.debug("No special nodes translated, content unchanged");
            } else {
                log.debug("Document rewrite completed. Original: {} chars, Translated: {} chars",
                        originalContent.length(), translatedContent.length());
            }

            return translatedContent;

        } catch (Exception e) {
            log.error("Error during document rewrite: {}", e.getMessage(), e);
            return originalContent; // 出错时返回原文
        }
    }

    /** 重写整个文档，递归处理所有节点
     * 
     * @param document AST 文档根节点
     * @param result 结果构建器
     * @param context 处理上下文 */
    private void rewriteDocument(Node document, StringBuilder result, ProcessingContext context) {
        // 递归处理文档的所有子节点
        for (Node child : document.getChildren()) {
            rewriteNode(child, result, context);
        }
    }

    /** 重写单个节点
     * 
     * @param node 当前节点
     * @param result 结果构建器
     * @param context 处理上下文 */
    private void rewriteNode(Node node, StringBuilder result, ProcessingContext context) {
        if (isSpecialNode(node)) {
            // 特殊节点：翻译后替换
            String translatedContent = translateSpecialNode(node, context);
            result.append(translatedContent);

        } else if (node instanceof Text) {
            // 纯文本节点：直接输出
            result.append(node.getChars().toString());

        } else if (isFormattingNode(node)) {
            // 格式化节点（粗体、斜体等）：保持格式并递归处理子节点
            String startMarkup = getNodeStartMarkup(node);
            String endMarkup = getNodeEndMarkup(node);

            result.append(startMarkup);
            for (Node child : node.getChildren()) {
                rewriteNode(child, result, context);
            }
            result.append(endMarkup);

        } else if (node instanceof SoftLineBreak) {
            // 软换行：转为空格
            result.append(" ");

        } else if (node instanceof HardLineBreak) {
            // 硬换行：保持换行
            result.append("\n");

        } else {
            // 其他节点：递归处理子节点
            for (Node child : node.getChildren()) {
                rewriteNode(child, result, context);
            }
        }
    }

    /** 判断节点是否为特殊节点 */
    private boolean isSpecialNode(Node node) {
        return node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock || node instanceof TableBlock
                || node instanceof Image || isFormulaNode(node);
    }

    /** 检查是否为公式节点（简单的文本模式识别） */
    private boolean isFormulaNode(Node node) {
        if (node instanceof Text) {
            String text = node.getChars().toString();
            return text.matches(".*\\$\\$[^$]*\\$\\$.*") || text.matches(".*\\$[^$]*\\$.*");
        }
        return false;
    }

    /** 判断是否为格式化节点 */
    private boolean isFormattingNode(Node node) {
        return node instanceof Emphasis || node instanceof StrongEmphasis || node instanceof Link
                || node instanceof Code || node instanceof Heading || node instanceof Paragraph
                || node instanceof ListItem || node instanceof BulletList || node instanceof OrderedList
                || node instanceof BlockQuote;
    }

    /** 翻译特殊节点 */
    private String translateSpecialNode(Node node, ProcessingContext context) {
        try {
            String originalContent = node.getChars().toString();

            // 直接传递AST节点给翻译服务
            String translatedContent = translatorService.translate(node, context);

            if (translatedContent != null && !translatedContent.equals(originalContent)) {
                log.debug("{} node translated: {} chars -> {} chars", node.getClass().getSimpleName(),
                        originalContent.length(), translatedContent.length());
                return translatedContent;
            } else {
                log.debug("No translation for {} node, using original content", node.getClass().getSimpleName());
                return originalContent;
            }

        } catch (Exception e) {
            log.warn("Failed to translate {} node: {}", node.getClass().getSimpleName(), e.getMessage());
            return node.getChars().toString(); // 返回原内容
        }
    }

    /** 获取节点开始标记 */
    private String getNodeStartMarkup(Node node) {
        if (node instanceof Emphasis) {
            return "*";
        } else if (node instanceof StrongEmphasis) {
            return "**";
        } else if (node instanceof Code) {
            return "`";
        } else if (node instanceof Link) {
            return "[";
        } else if (node instanceof Heading) {
            Heading heading = (Heading) node;
            return "#".repeat(heading.getLevel()) + " ";
        } else if (node instanceof BlockQuote) {
            return "> ";
        } else if (node instanceof ListItem) {
            if (node.getParent() instanceof OrderedList) {
                // 简化处理，使用1.
                return "1. ";
            } else {
                return "- ";
            }
        }
        return "";
    }

    /** 获取节点结束标记 */
    private String getNodeEndMarkup(Node node) {
        if (node instanceof Emphasis) {
            return "*";
        } else if (node instanceof StrongEmphasis) {
            return "**";
        } else if (node instanceof Code) {
            return "`";
        } else if (node instanceof Link) {
            Link link = (Link) node;
            return "](" + link.getUrl() + ")";
        } else if (node instanceof Heading) {
            return "\n";
        } else if (node instanceof Paragraph) {
            return "\n";
        } else if (node instanceof ListItem) {
            return "\n";
        } else if (node instanceof BlockQuote) {
            return "\n";
        }
        return "";
    }

    /** 提取节点的纯文本内容 */
    private String extractTextContent(Node node) {
        if (node == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        extractTextRecursively(node, text);
        return text.toString();
    }

    /** 递归提取文本内容 */
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
}