package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.util.Stack;

/** 文档树构建器
 * 
 * 职责： - 根据AST构建层次化的文档树 - 支持原始内容模式和常规处理模式 - 维护标题层级关系
 * 
 * @author claude */
@Component
public class DocumentTreeBuilder {

    private final MarkdownTextExtractor textExtractor;

    public DocumentTreeBuilder(MarkdownTextExtractor textExtractor) {
        this.textExtractor = textExtractor;
    }

    /** 构建保持原始内容的文档树
     * 
     * @param document AST根节点
     * @param splitConfig 分割配置
     * @return 文档树 */
    public DocumentTree buildRawDocumentTree(Node document, MarkdownProcessorProperties.SegmentSplit splitConfig) {
        DocumentTree tree = new DocumentTree(splitConfig);

        Stack<HeadingNode> nodeStack = new Stack<>();
        HeadingNode currentHeading = null;

        for (Node child : document.getChildren()) {
            if (child instanceof Heading) {
                // 处理标题节点
                Heading heading = (Heading) child;
                String headingText = textExtractor.extractTextContent(heading);
                HeadingNode newNode = new HeadingNode(heading.getLevel(), headingText);

                // 维护层级关系
                while (!nodeStack.isEmpty() && nodeStack.peek().getLevel() >= heading.getLevel()) {
                    nodeStack.pop();
                }

                if (nodeStack.isEmpty()) {
                    tree.addRootNode(newNode);
                } else {
                    nodeStack.peek().addChild(newNode);
                }

                nodeStack.push(newNode);
                currentHeading = newNode;

            } else {
                // 处理内容节点 - 保持原始格式
                if (currentHeading != null) {
                    String nodeContent = textExtractor.extractRawContent(child);
                    if (!nodeContent.trim().isEmpty()) {
                        currentHeading.addDirectContent(nodeContent);
                    }
                } else {
                    // 创建虚拟根节点
                    if (tree.getRootNodes().isEmpty()) {
                        HeadingNode virtualRoot = new HeadingNode(1, "文档内容");
                        tree.addRootNode(virtualRoot);
                        currentHeading = virtualRoot;
                        nodeStack.push(virtualRoot);
                    }

                    String nodeContent = textExtractor.extractRawContent(child);
                    if (!nodeContent.trim().isEmpty()) {
                        currentHeading.addDirectContent(nodeContent);
                    }
                }
            }
        }

        return tree;
    }

    /** 构建常规处理的文档树
     * 
     * @param document AST根节点
     * @param splitConfig 分割配置
     * @return 文档树 */
    public DocumentTree buildDocumentTree(Node document, MarkdownProcessorProperties.SegmentSplit splitConfig) {
        DocumentTree tree = new DocumentTree(splitConfig);

        // 使用栈来跟踪当前的标题层次
        Stack<HeadingNode> nodeStack = new Stack<>();
        HeadingNode currentHeading = null;

        for (Node child : document.getChildren()) {
            if (child instanceof Heading) {
                Heading heading = (Heading) child;
                String headingText = textExtractor.extractTextContent(heading);
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
                    String nodeContent = textExtractor.extractTextContent(child);
                    if (!nodeContent.trim().isEmpty()) {
                        currentHeading.addDirectContent(nodeContent);
                    }
                } else {
                    // 没有标题的内容，创建一个虚拟的根标题
                    if (tree.getRootNodes().isEmpty()) {
                        HeadingNode virtualRoot = new HeadingNode(1, "文档内容");
                        tree.addRootNode(virtualRoot);
                        currentHeading = virtualRoot;
                        nodeStack.push(virtualRoot);
                    }

                    String nodeContent = textExtractor.extractTextContent(child);
                    if (!nodeContent.trim().isEmpty()) {
                        currentHeading.addDirectContent(nodeContent);
                    }
                }
            }
        }

        return tree;
    }
}