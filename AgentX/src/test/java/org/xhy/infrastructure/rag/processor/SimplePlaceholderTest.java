package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 简单占位符测试 - 不依赖复杂的Flexmark导入 */
class SimplePlaceholderTest {

    private PureMarkdownProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PureMarkdownProcessor();
    }

    @Test
    void shouldProcessBasicMarkdown() {
        // Given
        String markdown = "# 标题\n\n这是基本内容。";
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = processor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        System.out.println("=== 基本Markdown测试 ===");
        System.out.println("段落数: " + segments.size());
        for (ProcessedSegment segment : segments) {
            System.out.println("类型: " + segment.getType());
            System.out.println("内容: " + segment.getContent());
            System.out.println("有特殊节点: " + segment.hasSpecialNodes());
        }
    }

    @Test
    void shouldProcessImageMarkdown() {
        // Given
        String markdown = "# 图片测试\n\n![示例图片](https://example.com/image.jpg)\n\n这是图片后的文字。";
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = processor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        System.out.println("=== 图片处理测试 ===");
        System.out.println("段落数: " + segments.size());

        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            System.out.println("\n段落 " + (i + 1) + ":");
            System.out.println("类型: " + segment.getType());
            System.out.println("内容: " + segment.getContent());
            System.out.println("有特殊节点: " + segment.hasSpecialNodes());

            if (segment.hasSpecialNodes()) {
                System.out.println("特殊节点数量: " + segment.getSpecialNodes().size());
                segment.getSpecialNodes().forEach((key, value) -> {
                    System.out.println("- 占位符: " + key);
                    System.out.println("- 节点类型: " + value.getNodeType());
                    System.out.println("- 原始内容: " + value.getOriginalContent());
                });
            }

            // 验证占位符内容
            if (segment.getContent().contains("{{SPECIAL_NODE_IMAGE")) {
                System.out.println("✓ 发现图片占位符");
            }

            // 验证最终内容
            String finalContent = segment.getFinalContent();
            System.out.println("最终内容: " + finalContent);
            if (finalContent.contains("![示例图片](https://example.com/image.jpg)")) {
                System.out.println("✓ 占位符替换成功");
            }
        }
    }

    @Test
    void shouldHandleCodeBlocks() {
        // Given
        String markdown = "# 代码测试\n\n```java\npublic class Test {}\n```\n\n代码后的内容。";
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = processor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        System.out.println("=== 代码处理测试 ===");

        for (ProcessedSegment segment : segments) {
            System.out.println("内容: " + segment.getContent());
            if (segment.hasSpecialNodes()) {
                System.out.println("特殊节点数量: " + segment.getSpecialNodes().size());
                if (segment.getSpecialNodeCount(SegmentType.CODE) > 0) {
                    System.out.println("✓ 发现代码占位符");
                }
            }
        }
    }
}