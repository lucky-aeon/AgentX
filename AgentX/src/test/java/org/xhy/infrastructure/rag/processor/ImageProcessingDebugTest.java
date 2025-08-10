package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.SpecialNode;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.List;

/** 图片处理调试测试 */
class ImageProcessingDebugTest {

    private PureMarkdownProcessor pureProcessor;

    @BeforeEach
    void setUp() {
        pureProcessor = new PureMarkdownProcessor();
    }

    @Test
    void debugImageProcessing() {
        // Given
        String markdown = """
                # 图片测试

                ![示例图片](https://example.com/image.jpg)

                这是图片后的文字。
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then - 调试输出
        System.out.println("=== 调试信息 ===");
        System.out.println("总段落数: " + segments.size());

        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            System.out.println("\n段落 " + (i + 1) + ":");
            System.out.println("类型: " + segment.getType());
            System.out.println("内容: " + segment.getContent());
            System.out.println("是否有特殊节点: " + segment.hasSpecialNodes());

            if (segment.hasSpecialNodes()) {
                System.out.println("特殊节点数量: " + segment.getSpecialNodes().size());
                for (SpecialNode node : segment.getSpecialNodes().values()) {
                    System.out.println("- 节点类型: " + node.getNodeType());
                    System.out.println("- 占位符: " + node.getPlaceholder());
                    System.out.println("- 原始内容: " + node.getOriginalContent());
                }
            }

            System.out.println("最终内容: " + segment.getFinalContent());
        }
    }

    @Test
    void debugSimpleImage() {
        // Given - 更简单的测试
        String markdown = "![test](url)";
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then
        System.out.println("=== 简单图片测试 ===");
        System.out.println("段落数: " + segments.size());

        if (!segments.isEmpty()) {
            ProcessedSegment segment = segments.get(0);
            System.out.println("段落类型: " + segment.getType());
            System.out.println("内容: '" + segment.getContent() + "'");
            System.out.println("有特殊节点: " + segment.hasSpecialNodes());

            if (segment.hasSpecialNodes()) {
                System.out.println("特殊节点: " + segment.getSpecialNodes().keySet());
            }
        }
    }
}