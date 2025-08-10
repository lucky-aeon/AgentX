package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.junit.jupiter.api.Test;

/** 详细调试测试 */
class DetailedDebugTest {

    @Test
    void debugFlexmarkParsing() {
        // Given
        String markdown = "![test](url)";

        // 配置Flexmark解析器（复制自PureMarkdownProcessor）
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();

        // When
        Node document = parser.parse(markdown);

        // Then - 遍历所有节点
        System.out.println("=== Flexmark AST 结构 ===");
        printNodeTree(document, 0);
    }

    @Test
    void debugWithHeading() {
        // Given
        String markdown = """
                # 标题

                ![test](url)
                """;

        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();

        // When
        Node document = parser.parse(markdown);

        // Then
        System.out.println("=== 带标题的AST结构 ===");
        printNodeTree(document, 0);
    }

    private void printNodeTree(Node node, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "- " + node.getClass().getSimpleName() + ": '"
                + node.getChars().toString().replace("\n", "\\n") + "'");

        if (node instanceof Image) {
            Image image = (Image) node;
            System.out.println(indent + "  URL: " + image.getUrl());
            System.out.println(indent + "  Title: " + image.getTitle());
        }

        for (Node child : node.getChildren()) {
            printNodeTree(child, depth + 1);
        }
    }
}