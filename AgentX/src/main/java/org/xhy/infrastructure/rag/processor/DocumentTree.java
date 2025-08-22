package org.xhy.infrastructure.rag.processor;

import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/** 文档树 - 表示整个Markdown文档的层次结构
 * 
 * 负责构建标题层次树和执行递归分割算法 */
public class DocumentTree {

    /** 根节点列表（顶级标题） */
    private List<HeadingNode> rootNodes;

    /** 分割配置 */
    private MarkdownProcessorProperties.SegmentSplit config;

    public DocumentTree(MarkdownProcessorProperties.SegmentSplit config) {
        this.rootNodes = new ArrayList<>();
        this.config = config;
    }

    /** 添加根节点 */
    public void addRootNode(HeadingNode node) {
        if (node != null) {
            rootNodes.add(node);
        }
    }

    /** 获取所有根节点 */
    public List<HeadingNode> getRootNodes() {
        return new ArrayList<>(rootNodes);
    }

    /** 执行递归分层分割算法 */
    public List<ProcessedSegment> performHierarchicalSplit() {
        List<ProcessedSegment> results = new ArrayList<>();

        for (HeadingNode rootNode : rootNodes) {
            List<ProcessedSegment> rootSegments = splitNodeRecursively(rootNode);
            results.addAll(rootSegments);
        }

        return results;
    }

    /** 递归分割单个标题节点 */
    private List<ProcessedSegment> splitNodeRecursively(HeadingNode node) {
        List<ProcessedSegment> results = new ArrayList<>();

        // 如果节点总长度在限制内，直接返回完整内容
        if (node.getTotalContentLength() <= config.getMaxLength()) {
            ProcessedSegment completeSegment = createSegmentFromNode(node);
            results.add(completeSegment);
            return results;
        }

        // 节点内容超长，需要分割

        // 策略1: 如果有子节点，按子节点分割
        if (!node.getChildren().isEmpty()) {
            results.addAll(splitByChildren(node));
        } else {
            // 策略2: 叶子节点，按段落分割
            results.addAll(splitByParagraphs(node));
        }

        return results;
    }

    /** 按子节点分割 */
    private List<ProcessedSegment> splitByChildren(HeadingNode node) {
        List<ProcessedSegment> results = new ArrayList<>();

        // 处理节点的直接内容（如果有）
        if (node.hasDirectContent()) {
            ProcessedSegment directSegment = node.generateDirectContentSegment();
            if (directSegment.getContent().length() <= config.getMaxLength()) {
                results.add(directSegment);
            } else {
                // 直接内容也超长，需要段落分割
                results.addAll(splitSegmentByParagraphs(directSegment, node.getFullTitlePath()));
            }
        }

        // 递归处理每个子节点
        for (HeadingNode child : node.getChildren()) {
            List<ProcessedSegment> childSegments = splitNodeRecursively(child);
            results.addAll(childSegments);
        }

        return results;
    }

    /** 按段落分割叶子节点 */
    private List<ProcessedSegment> splitByParagraphs(HeadingNode node) {
        ProcessedSegment nodeSegment = createSegmentFromNode(node);
        String titlePath = node.getFullTitlePath();

        return splitSegmentByParagraphs(nodeSegment, titlePath);
    }

    /** 按段落分割单个段落对象 */
    private List<ProcessedSegment> splitSegmentByParagraphs(ProcessedSegment segment, String titlePath) {
        List<ProcessedSegment> results = new ArrayList<>();

        // 提取内容部分（去掉标题前缀）
        String content = segment.getContent();
        String contentWithoutTitle = extractContentWithoutTitle(content, titlePath);

        // 计算标题前缀长度
        int titlePrefixLength = titlePath.length() + 2; // +2 for "\n\n"
        int availableLength = config.getMaxLength() - titlePrefixLength - config.getBufferSize();

        if (availableLength <= config.getMinLength()) {
            // 标题太长，直接截断
            String truncated = content.substring(0,
                    Math.min(content.length(), config.getMaxLength() - config.getBufferSize()));
            ProcessedSegment truncatedSegment = new ProcessedSegment(truncated, segment.getType(),
                    segment.getMetadata());
            results.add(truncatedSegment);
            return results;
        }

        // 按段落分割内容
        String[] paragraphs = contentWithoutTitle.split("\n\n");
        StringBuilder currentContent = new StringBuilder();
        int currentLength = 0;

        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }

            int paragraphLength = trimmedParagraph.length() + 2; // +2 for "\n\n"

            if (currentLength + paragraphLength <= availableLength) {
                // 可以添加到当前段落
                if (currentContent.length() > 0) {
                    currentContent.append("\n\n");
                }
                currentContent.append(trimmedParagraph);
                currentLength += paragraphLength;
            } else {
                // 当前段落已满，保存并开始新段落
                if (currentContent.length() > 0) {
                    ProcessedSegment newSegment = createSegmentWithTitle(titlePath, currentContent.toString(), segment);
                    results.add(newSegment);
                    currentContent.setLength(0);
                    currentLength = 0;
                }

                // 处理当前段落
                if (paragraphLength <= availableLength) {
                    // 当前段落可以作为新段落的开始
                    currentContent.append(trimmedParagraph);
                    currentLength = paragraphLength;
                } else {
                    // 单个段落超长，截断处理
                    String truncated = trimmedParagraph.substring(0, availableLength - config.getBufferSize());
                    ProcessedSegment truncatedSegment = createSegmentWithTitle(titlePath, truncated, segment);
                    results.add(truncatedSegment);
                }
            }
        }

        // 保存最后一个段落
        if (currentContent.length() > 0) {
            ProcessedSegment finalSegment = createSegmentWithTitle(titlePath, currentContent.toString(), segment);
            results.add(finalSegment);
        }

        // 如果没有生成任何段落，至少返回一个截断的段落
        if (results.isEmpty()) {
            String fallback = content.substring(0,
                    Math.min(content.length(), config.getMaxLength() - config.getBufferSize()));
            ProcessedSegment fallbackSegment = new ProcessedSegment(fallback, segment.getType(), segment.getMetadata());
            results.add(fallbackSegment);
        }

        return results;
    }

    /** 从标题节点创建ProcessedSegment */
    private ProcessedSegment createSegmentFromNode(HeadingNode node) {
        String fullContent = node.generateFullContent();
        ProcessedSegment segment = new ProcessedSegment(fullContent, org.xhy.domain.rag.model.enums.SegmentType.SECTION,
                null);

        return segment;
    }

    /** 提取不包含标题的内容部分 */
    private String extractContentWithoutTitle(String fullContent, String titlePath) {
        if (titlePath == null || titlePath.trim().isEmpty()) {
            return fullContent;
        }

        // 找到标题结束的位置
        int titleEndIndex = fullContent.indexOf("\n\n");
        if (titleEndIndex > 0) {
            String actualTitlePart = fullContent.substring(0, titleEndIndex);
            if (actualTitlePart.trim().equals(titlePath.trim())) {
                return fullContent.substring(titleEndIndex + 2).trim();
            }
        }

        return fullContent;
    }

    /** 创建带标题前缀的段落 */
    private ProcessedSegment createSegmentWithTitle(String titlePrefix, String content,
            ProcessedSegment originalSegment) {
        StringBuilder fullContent = new StringBuilder();

        if (titlePrefix != null && !titlePrefix.trim().isEmpty()) {
            fullContent.append(titlePrefix).append("\n\n");
        }
        fullContent.append(content);

        ProcessedSegment newSegment = new ProcessedSegment(fullContent.toString(), originalSegment.getType(),
                originalSegment.getMetadata());

        return newSegment;
    }

    /** 获取文档树的统计信息 */
    public String getTreeStatistics() {
        int totalNodes = 0;
        int maxDepth = 0;

        for (HeadingNode root : rootNodes) {
            totalNodes += countNodes(root);
            maxDepth = Math.max(maxDepth, getDepth(root, 1));
        }

        return String.format("DocumentTree{rootNodes=%d, totalNodes=%d, maxDepth=%d}", rootNodes.size(), totalNodes,
                maxDepth);
    }

    /** 递归计算节点数量 */
    private int countNodes(HeadingNode node) {
        int count = 1; // 当前节点
        for (HeadingNode child : node.getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    /** 计算树的最大深度 */
    private int getDepth(HeadingNode node, int currentDepth) {
        int maxChildDepth = currentDepth;
        for (HeadingNode child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, getDepth(child, currentDepth + 1));
        }
        return maxChildDepth;
    }

    @Override
    public String toString() {
        return getTreeStatistics();
    }
}