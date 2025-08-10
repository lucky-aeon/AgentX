package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 占位符架构测试 */
class PlaceholderArchitectureTest {

    private PureMarkdownProcessor pureProcessor;

    @BeforeEach
    void setUp() {
        pureProcessor = new PureMarkdownProcessor();
    }

    @Test
    void shouldCreatePlaceholdersForImages() {
        // Given
        String markdown = """
                # 图片测试

                ![示例图片](https://example.com/image.jpg)

                这是图片后的文字。
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();

        ProcessedSegment mainSegment = segments.get(0);

        // 验证段落类型
        assertThat(mainSegment.getType()).isEqualTo(SegmentType.SECTION);

        // 验证特殊节点
        assertThat(mainSegment.hasSpecialNodes()).isTrue();
        assertThat(mainSegment.getSpecialNodeCount(SegmentType.IMAGE)).isEqualTo(1);

        // 验证占位符
        assertThat(mainSegment.getContent()).contains("{{SPECIAL_NODE_IMAGE_001}}");

        // 验证最终内容（应用占位符替换后）
        String finalContent = mainSegment.getFinalContent();
        assertThat(finalContent).contains("![示例图片](https://example.com/image.jpg)");
        assertThat(finalContent).doesNotContain("{{SPECIAL_NODE_IMAGE_001}}");

        System.out.println("=== 占位符内容 ===");
        System.out.println(mainSegment.getContent());
        System.out.println("\n=== 最终内容 ===");
        System.out.println(finalContent);
    }

    @Test
    void shouldCreatePlaceholdersForTables() {
        // Given
        String markdown = """
                # 表格测试

                | 名称 | 年龄 | 职业 |
                |------|------|------|
                | 张三 | 25   | 程序员 |
                | 李四 | 30   | 设计师 |

                表格后的内容。
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();

        ProcessedSegment mainSegment = segments.get(0);

        // 验证特殊节点
        assertThat(mainSegment.hasSpecialNodes()).isTrue();
        assertThat(mainSegment.getSpecialNodeCount(SegmentType.TABLE)).isEqualTo(1);

        // 验证占位符
        assertThat(mainSegment.getContent()).contains("{{SPECIAL_NODE_TABLE_001}}");

        // 验证最终内容
        String finalContent = mainSegment.getFinalContent();
        assertThat(finalContent).contains("张三");
        assertThat(finalContent).contains("程序员");

        System.out.println("=== 表格段落 ===");
        System.out.println("占位符内容: " + mainSegment.getContent());
        System.out.println("最终内容: " + finalContent);
    }

    @Test
    void shouldCreatePlaceholdersForCode() {
        // Given
        String markdown = """
                # 代码测试

                ```java
                public class Test {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
                ```

                代码后的内容。
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();

        ProcessedSegment mainSegment = segments.get(0);

        // 验证特殊节点
        assertThat(mainSegment.hasSpecialNodes()).isTrue();
        assertThat(mainSegment.getSpecialNodeCount(SegmentType.CODE)).isEqualTo(1);

        // 验证占位符
        assertThat(mainSegment.getContent()).contains("{{SPECIAL_NODE_CODE_001}}");

        // 验证最终内容
        String finalContent = mainSegment.getFinalContent();
        assertThat(finalContent).contains("public class Test");
        assertThat(finalContent).contains("Hello World");

        System.out.println("=== 代码段落 ===");
        System.out.println("占位符内容: " + mainSegment.getContent());
        System.out.println("最终内容: " + finalContent);
    }

    @Test
    void shouldHandleMixedContent() {
        // Given
        String markdown = """
                # 混合内容测试

                这是文字内容。

                ![图片](https://example.com/image.jpg)

                这是表格：

                | 列1 | 列2 |
                |-----|-----|
                | A   | B   |

                ```python
                print("Hello")
                ```

                结束文字。
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();

        ProcessedSegment mainSegment = segments.get(0);

        // 验证所有特殊节点都被正确识别
        assertThat(mainSegment.hasSpecialNodes()).isTrue();
        assertThat(mainSegment.getSpecialNodeCount(SegmentType.IMAGE)).isEqualTo(1);
        assertThat(mainSegment.getSpecialNodeCount(SegmentType.TABLE)).isEqualTo(1);
        assertThat(mainSegment.getSpecialNodeCount(SegmentType.CODE)).isEqualTo(1);

        // 验证占位符
        String content = mainSegment.getContent();
        assertThat(content).contains("{{SPECIAL_NODE_IMAGE_001}}");
        assertThat(content).contains("{{SPECIAL_NODE_TABLE_001}}");
        assertThat(content).contains("{{SPECIAL_NODE_CODE_001}}");

        // 验证最终内容
        String finalContent = mainSegment.getFinalContent();
        assertThat(finalContent).contains("![图片](https://example.com/image.jpg)");
        assertThat(finalContent).contains("| 列1 | 列2 |");
        assertThat(finalContent).contains("print(\"Hello\")");

        System.out.println("=== 混合内容测试 ===");
        System.out.println("特殊节点数量: " + mainSegment.getSpecialNodes().size());
        System.out.println("占位符内容长度: " + content.length());
        System.out.println("最终内容长度: " + finalContent.length());
    }
}