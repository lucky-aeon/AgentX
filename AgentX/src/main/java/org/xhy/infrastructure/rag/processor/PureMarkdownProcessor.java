package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 纯净Markdown处理器
 * 
 * 设计职责：
 * - 只负责Markdown结构化解析和语义分段
 * - 不进行任何LLM调用或内容增强
 * - 按语义结构（标题层级）进行合理分段
 * - 提取基础元数据，保留原始内容
 * 
 * 适用场景：
 * - 单元测试（无需mock外部服务）
 * - 基础文档解析
 * - 性能要求高的场景
 * 
 * @author claude */
@Component("pureMarkdownProcessor")
public class PureMarkdownProcessor implements MarkdownProcessor {

    private static final Logger log = LoggerFactory.getLogger(PureMarkdownProcessor.class);

    private final Parser parser;

    public PureMarkdownProcessor() {
        // 配置Flexmark解析器
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
    }

    @Override
    public List<ProcessedSegment> processToSegments(String markdown, ProcessingContext context) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 解析Markdown为AST
            Node document = parser.parse(markdown);

            List<ProcessedSegment> segments = new ArrayList<>();
            int order = 0;

            // 使用语义感知遍历进行分段
            order = processSemanticStructure(document, segments, order);

            log.info("Pure processing completed: {} segments generated", segments.size());
            return segments;

        } catch (Exception e) {
            log.error("Failed to process markdown with pure processor", e);
            // 回退方案：整个文档作为一个段落
            ProcessedSegment fallback = new ProcessedSegment(markdown, "text", null);
            fallback.setOrder(0);
            return List.of(fallback);
        }
    }

    /** 语义结构处理 - 以标题为分段边界，聚合标题下所有内容 */
    private int processSemanticStructure(Node document, List<ProcessedSegment> segments, int order) {

        List<Node> children = new ArrayList<>();
        for (Node child : document.getChildren()) {
            children.add(child);
        }

        int currentOrder = order;
        StringBuilder currentSectionContent = new StringBuilder();
        boolean inHeadingSection = false;

        for (Node child : children) {
            if (child instanceof Heading) {
                // 任何标题都是分段边界
                if (currentSectionContent.length() > 0) {
                    // 保存上一个段落
                    ProcessedSegment section = new ProcessedSegment(currentSectionContent.toString().trim(), "section", null);
                    section.setOrder(currentOrder++);
                    segments.add(section);
                    currentSectionContent.setLength(0);
                }

                // 开始新段落：添加标题文本
                Heading heading = (Heading) child;
                String headingPrefix = "#".repeat(heading.getLevel());
                String headingText = extractTextContent(heading);
                currentSectionContent.append(headingPrefix).append(" ").append(headingText).append("\n\n");
                inHeadingSection = true;

            } else {
                // 所有非标题内容：归属于当前标题段落
                if (inHeadingSection) {
                    String nodeContent = processNodeContent(child);
                    if (!nodeContent.trim().isEmpty()) {
                        currentSectionContent.append(nodeContent).append("\n\n");
                    }
                } else {
                    // 没有标题的独立内容
                    ProcessedSegment standalone = processStandaloneNode(child);
                    if (standalone != null) {
                        standalone.setOrder(currentOrder++);
                        segments.add(standalone);
                    }
                }
            }
        }

        // 保存最后一个段落
        if (currentSectionContent.length() > 0) {
            ProcessedSegment section = new ProcessedSegment(currentSectionContent.toString().trim(), "section", null);
            section.setOrder(currentOrder++);
            segments.add(section);
        }

        return currentOrder;
    }

    /** 处理节点内容，归属于当前标题段落 */
    private String processNodeContent(Node node) {
        if (isSpecialNode(node)) {
            // 对特殊节点进行结构化处理，但不调用LLM
            ProcessedSegment structuredSegment = processSpecialNodePure(node);
            if (structuredSegment != null) {
                return structuredSegment.getContent();
            }
        }
        
        // 普通节点：提取文本内容
        return extractTextContent(node);
    }

    /** 判断是否为特殊节点（代码块、表格等） */
    private boolean isSpecialNode(Node node) {
        return node instanceof FencedCodeBlock || 
               node instanceof IndentedCodeBlock ||
               node instanceof com.vladsch.flexmark.ext.tables.TableBlock ||
               node instanceof Image;
    }

    /** 纯净处理特殊节点 - 只做结构解析，不调用LLM */
    private ProcessedSegment processSpecialNodePure(Node node) {
        try {
            if (node instanceof FencedCodeBlock) {
                return processCodeBlockPure((FencedCodeBlock) node);
            } else if (node instanceof IndentedCodeBlock) {
                return processCodeBlockPure((IndentedCodeBlock) node);
            } else if (node instanceof com.vladsch.flexmark.ext.tables.TableBlock) {
                return processTablePure((com.vladsch.flexmark.ext.tables.TableBlock) node);
            } else if (node instanceof Image) {
                return processImagePure((Image) node);
            }
        } catch (Exception e) {
            log.warn("Failed to process special node purely: {}", e.getMessage());
        }

        // 回退：返回原始文本
        String rawText = node.getChars().toString();
        return new ProcessedSegment(rawText, "raw", null);
    }

    /** 纯净处理代码块 */
    private ProcessedSegment processCodeBlockPure(Node codeNode) {
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

        // 构建结构化元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "code");
        metadata.put("language", language != null ? language : "unknown");
        metadata.put("lines", codeContent.split("\n").length);

        // 保留原始格式化内容
        String displayContent = String.format("```%s\n%s\n```", language != null ? language : "", codeContent);
        
        return new ProcessedSegment(displayContent, "code", metadata);
    }

    /** 纯净处理表格 */
    private ProcessedSegment processTablePure(com.vladsch.flexmark.ext.tables.TableBlock tableBlock) {
        String tableContent = tableBlock.getChars().toString();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "table");
        
        return new ProcessedSegment(tableContent, "table", metadata);
    }

    /** 纯净处理图片 */
    private ProcessedSegment processImagePure(Image image) {
        String imageUrl = image.getUrl().toString();
        String altText = extractTextContent(image);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "image");
        metadata.put("url", imageUrl);
        metadata.put("alt", altText);
        
        String displayContent = String.format("![%s](%s)", altText, imageUrl);
        
        return new ProcessedSegment(displayContent, "image", metadata);
    }

    /** 处理没有标题的独立节点 */
    private ProcessedSegment processStandaloneNode(Node node) {
        if (isSpecialNode(node)) {
            // 特殊节点作为独立段落
            return processSpecialNodePure(node);
        } else {
            // 普通内容作为独立段落
            String nodeText = extractTextContent(node);
            if (!nodeText.trim().isEmpty()) {
                return new ProcessedSegment(nodeText.trim(), "text", null);
            }
        }
        return null;
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
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            text.append(" ");
        } else if (node instanceof Code) {
            text.append("`").append(node.getChars().toString()).append("`");
        } else if (node instanceof Emphasis) {
            text.append("*");
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
            text.append("*");
        } else if (node instanceof StrongEmphasis) {
            text.append("**");
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
            text.append("**");
        } else if (node instanceof Link) {
            Link link = (Link) node;
            text.append("[");
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
            text.append("](").append(link.getUrl()).append(")");
        } else if (node instanceof Image) {
            Image image = (Image) node;
            String altText = "";
            // 提取alt text
            for (Node child : image.getChildren()) {
                StringBuilder altBuilder = new StringBuilder();
                extractTextRecursively(child, altBuilder);
                altText = altBuilder.toString();
            }
            text.append("![").append(altText).append("](").append(image.getUrl()).append(")");
        } else if (node instanceof BulletList) {
            // 无序列表
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
        } else if (node instanceof OrderedList) {
            // 有序列表
            OrderedList orderedList = (OrderedList) node;
            int itemNumber = orderedList.getStartNumber();
            for (Node child : node.getChildren()) {
                if (child instanceof ListItem) {
                    text.append(itemNumber++).append(". ");
                    extractTextRecursively(child, text);
                } else {
                    extractTextRecursively(child, text);
                }
            }
        } else if (node instanceof ListItem) {
            ListItem listItem = (ListItem) node;
            // 根据父节点判断是有序还是无序列表
            if (!(listItem.getParent() instanceof OrderedList)) {
                text.append("- ");
            }
            // 处理列表项内容
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
            text.append("\n");
        } else if (node instanceof BlockQuote) {
            // 引用块
            text.append("> ");
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
        } else {
            // 其他节点，递归处理子节点
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
        }
    }
}