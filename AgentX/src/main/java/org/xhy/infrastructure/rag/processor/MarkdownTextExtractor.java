package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;

/** Markdown 文本提取器
 * 
 * 职责： - 从AST节点提取纯文本内容 - 从AST节点提取原始内容（保持格式） - 递归处理嵌套节点结构
 * 
 * @author claude */
@Component
public class MarkdownTextExtractor {

    /** 提取节点的纯文本内容
     * 
     * @param node AST节点
     * @return 纯文本内容 */
    public String extractTextContent(Node node) {
        if (node == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        extractTextRecursively(node, text);
        return text.toString();
    }

    /** 提取节点的原始内容（保持格式）
     * 
     * @param node AST节点
     * @return 原始内容 */
    public String extractRawContent(Node node) {
        if (node == null) {
            return "";
        }
        // 直接返回节点的原始markdown内容
        return node.getChars().toString();
    }

    /** 递归提取文本内容
     * 
     * @param node 当前节点
     * @param text 文本构建器 */
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