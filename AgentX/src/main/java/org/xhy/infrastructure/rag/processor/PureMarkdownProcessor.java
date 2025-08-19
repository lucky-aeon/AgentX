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
import org.xhy.domain.rag.model.SpecialNode;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 纯净Markdown处理器
 * 
 * 设计职责： - 只负责Markdown结构化解析和语义分段 - 不进行任何LLM调用或内容增强 - 按语义结构（标题层级）进行合理分段 - 提取基础元数据，保留原始内容
 * 
 * 适用场景： - 单元测试（无需mock外部服务） - 基础文档解析 - 性能要求高的场景
 * 
 * @author claude */
@Component("pureMarkdownProcessor")
public class PureMarkdownProcessor implements MarkdownProcessor {

    private static final Logger log = LoggerFactory.getLogger(PureMarkdownProcessor.class);

    private final Parser parser;
    private MarkdownProcessorProperties markdownProperties;

    // 占位符计数器
    private final AtomicInteger imageCounter = new AtomicInteger(1);
    private final AtomicInteger codeCounter = new AtomicInteger(1);
    private final AtomicInteger tableCounter = new AtomicInteger(1);
    private final AtomicInteger formulaCounter = new AtomicInteger(1);

    public PureMarkdownProcessor(MarkdownProcessorProperties markdownProperties) {
        this.markdownProperties = markdownProperties;
        
        // 配置Flexmark解析器
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
    }

    /** 默认构造函数，使用层次化分割的推荐配置 */
    public PureMarkdownProcessor() {
        // 使用层次化分割的默认配置
        this.markdownProperties = new MarkdownProcessorProperties();
        MarkdownProcessorProperties.SegmentSplit segmentSplit = new MarkdownProcessorProperties.SegmentSplit();
        segmentSplit.setEnabled(true);       // 启用层次化分割
        segmentSplit.setMaxLength(1800);     // 默认最大长度
        segmentSplit.setMinLength(200);      // 默认最小长度  
        segmentSplit.setBufferSize(100);     // 默认缓冲区
        this.markdownProperties.setSegmentSplit(segmentSplit);
        
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
            ProcessedSegment fallback = new ProcessedSegment(markdown, SegmentType.TEXT, null);
            fallback.setOrder(0);
            return List.of(fallback);
        }
    }

    /** 语义结构处理 - 构建文档树并执行层次化分割 */
    private int processSemanticStructure(Node document, List<ProcessedSegment> segments, int order) {
        // 构建文档树
        DocumentTree documentTree = buildDocumentTree(document);
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


    /** 构建文档的标题层次树 */
    private DocumentTree buildDocumentTree(Node document) {
        DocumentTree tree = new DocumentTree(markdownProperties.getSegmentSplit());
        
        // 使用栈来跟踪当前的标题层次
        Stack<HeadingNode> nodeStack = new Stack<>();
        HeadingNode currentHeading = null;
        
        for (Node child : document.getChildren()) {
            if (child instanceof Heading) {
                Heading heading = (Heading) child;
                String headingText = extractTextContent(heading);
                HeadingNode newNode = new HeadingNode(heading.getLevel(), headingText);
                
                // 找到合适的父节点
                while (!nodeStack.isEmpty() && nodeStack.peek().getLevel() >= heading.getLevel()) {
                    nodeStack.pop();
                }
                
                if (nodeStack.isEmpty()) {
                    // 这是一个根级别的标题
                    tree.addRootNode(newNode);
                } else {
                    // 添加到父节点的子节点列表
                    nodeStack.peek().addChild(newNode);
                }
                
                nodeStack.push(newNode);
                currentHeading = newNode;
                
            } else {
                // 非标题内容，添加到当前标题下
                if (currentHeading != null) {
                    // 处理特殊节点
                    if (isSpecialNode(child)) {
                        SpecialNode specialNode = createSpecialNodeFromNode(child);
                        if (specialNode != null) {
                            currentHeading.addSpecialNode(specialNode);
                            // 添加占位符到内容中
                            currentHeading.addDirectContent(specialNode.getPlaceholder());
                        }
                    } else {
                        String nodeContent = extractTextContent(child);
                        if (!nodeContent.trim().isEmpty()) {
                            currentHeading.addDirectContent(nodeContent);
                        }
                    }
                } else {
                    // 没有标题的内容，创建一个虚拟的根标题
                    if (tree.getRootNodes().isEmpty()) {
                        HeadingNode virtualRoot = new HeadingNode(1, "文档内容");
                        tree.addRootNode(virtualRoot);
                        currentHeading = virtualRoot;
                        nodeStack.push(virtualRoot);
                    }
                    
                    // 处理特殊节点
                    if (isSpecialNode(child)) {
                        SpecialNode specialNode = createSpecialNodeFromNode(child);
                        if (specialNode != null) {
                            currentHeading.addSpecialNode(specialNode);
                            currentHeading.addDirectContent(specialNode.getPlaceholder());
                        }
                    } else {
                        String nodeContent = extractTextContent(child);
                        if (!nodeContent.trim().isEmpty()) {
                            currentHeading.addDirectContent(nodeContent);
                        }
                    }
                }
            }
        }
        
        return tree;
    }

    /** 从Flexmark节点创建特殊节点对象 */
    private SpecialNode createSpecialNodeFromNode(Node node) {
        SegmentType nodeType = determineNodeType(node);
        String placeholder = generatePlaceholder(nodeType);
        String originalContent = getOriginalNodeContent(node);
        
        SpecialNode specialNode = new SpecialNode(nodeType, placeholder, originalContent);
        
        // 收集节点元数据
        Map<String, Object> nodeMetadata = extractNodeMetadata(node);
        specialNode.setNodeMetadata(nodeMetadata);
        
        log.debug("Created special node {} for {} type", placeholder, nodeType);
        return specialNode;
    }

    /** 处理节点内容，归属于当前标题段落（占位符版本） */
    private String processNodeContentWithPlaceholders(Node node, ProcessedSegment currentSection) {
        if (isSpecialNode(node)) {
            // 对特殊节点生成占位符并收集节点信息
            return createPlaceholderForSpecialNode(node, currentSection);
        }

        // 检查是否为包含特殊节点的容器（如Paragraph）
        if (containsSpecialNodes(node)) {
            return processContainerNodeWithPlaceholders(node, currentSection);
        }

        // 普通节点：提取文本内容
        return extractTextContent(node);
    }

    /** 检查节点是否包含特殊子节点 */
    private boolean containsSpecialNodes(Node node) {
        if (isSpecialNode(node)) {
            return true;
        }

        for (Node child : node.getChildren()) {
            if (containsSpecialNodes(child)) {
                return true;
            }
        }

        return false;
    }

    /** 处理包含特殊节点的容器节点 */
    private String processContainerNodeWithPlaceholders(Node containerNode, ProcessedSegment currentSection) {
        StringBuilder result = new StringBuilder();

        for (Node child : containerNode.getChildren()) {
            if (isSpecialNode(child)) {
                // 直接处理特殊节点
                String placeholder = createPlaceholderForSpecialNode(child, currentSection);
                result.append(placeholder);
            } else if (containsSpecialNodes(child)) {
                // 递归处理包含特殊节点的子容器
                String childContent = processContainerNodeWithPlaceholders(child, currentSection);
                result.append(childContent);
            } else {
                // 普通子节点，提取文本内容
                String textContent = extractTextContent(child);
                result.append(textContent);
            }
        }

        return result.toString();
    }

    /** 为特殊节点创建占位符并收集节点信息 */
    private String createPlaceholderForSpecialNode(Node node, ProcessedSegment currentSection) {
        SegmentType nodeType = determineNodeType(node);
        String placeholder = generatePlaceholder(nodeType);
        String originalContent = getOriginalNodeContent(node);

        // 创建特殊节点对象
        SpecialNode specialNode = new SpecialNode(nodeType, placeholder, originalContent);

        // 收集节点元数据
        Map<String, Object> nodeMetadata = extractNodeMetadata(node);
        specialNode.setNodeMetadata(nodeMetadata);

        // 添加到当前段落
        currentSection.addSpecialNode(specialNode);

        log.debug("Created placeholder {} for {} node", placeholder, nodeType);

        return placeholder;
    }

    /** 确定节点类型 */
    private SegmentType determineNodeType(Node node) {
        if (node instanceof Image) {
            return SegmentType.IMAGE;
        } else if (node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
            return SegmentType.CODE;
        } else if (node instanceof com.vladsch.flexmark.ext.tables.TableBlock) {
            return SegmentType.TABLE;
        } else {
            return SegmentType.RAW; // 未知特殊节点
        }
    }

    /** 生成占位符 */
    private String generatePlaceholder(SegmentType nodeType) {
        switch (nodeType) {
            case IMAGE :
                return SpecialNode.generatePlaceholder(SegmentType.IMAGE, imageCounter.getAndIncrement());
            case CODE :
                return SpecialNode.generatePlaceholder(SegmentType.CODE, codeCounter.getAndIncrement());
            case TABLE :
                return SpecialNode.generatePlaceholder(SegmentType.TABLE, tableCounter.getAndIncrement());
            case FORMULA :
                return SpecialNode.generatePlaceholder(SegmentType.FORMULA, formulaCounter.getAndIncrement());
            default :
                return "{{SPECIAL_NODE_UNKNOWN_001}}";
        }
    }

    /** 获取节点的原始内容 */
    private String getOriginalNodeContent(Node node) {
        if (node instanceof Image) {
            return processImagePure((Image) node).getContent();
        } else if (node instanceof FencedCodeBlock) {
            return processCodeBlockPure((FencedCodeBlock) node).getContent();
        } else if (node instanceof IndentedCodeBlock) {
            return processCodeBlockPure((IndentedCodeBlock) node).getContent();
        } else if (node instanceof com.vladsch.flexmark.ext.tables.TableBlock) {
            return processTablePure((com.vladsch.flexmark.ext.tables.TableBlock) node).getContent();
        } else {
            return node.getChars().toString();
        }
    }

    /** 提取节点元数据 */
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
        }

        return metadata;
    }

    /** 判断是否为特殊节点（代码块、表格等） */
    private boolean isSpecialNode(Node node) {
        return node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock
                || node instanceof com.vladsch.flexmark.ext.tables.TableBlock || node instanceof Image;
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
        return new ProcessedSegment(rawText, SegmentType.RAW, null);
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

        return new ProcessedSegment(displayContent, SegmentType.CODE, metadata);
    }

    /** 纯净处理表格 */
    private ProcessedSegment processTablePure(com.vladsch.flexmark.ext.tables.TableBlock tableBlock) {
        String tableContent = tableBlock.getChars().toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "table");

        return new ProcessedSegment(tableContent, SegmentType.TABLE, metadata);
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

        return new ProcessedSegment(displayContent, SegmentType.IMAGE, metadata);
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
                return new ProcessedSegment(nodeText.trim(), SegmentType.TEXT, null);
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