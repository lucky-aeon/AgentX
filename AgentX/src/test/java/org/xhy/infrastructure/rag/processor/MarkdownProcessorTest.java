package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.MarkdownTokenProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Markdown处理器测试 */
@ExtendWith(MockitoExtension.class)
class MarkdownProcessorTest {

    @Mock
    private MarkdownTokenProcessor mockProcessor;

    private EnhancedMarkdownProcessor enhancedMarkdownProcessor;

    @BeforeEach
    void setUp() {
        // 创建配置
        MarkdownProcessorProperties properties = new MarkdownProcessorProperties();
        MarkdownProcessorProperties.SegmentSplit segmentSplit = new MarkdownProcessorProperties.SegmentSplit();
        segmentSplit.setEnabled(false); // 在这些测试中禁用拆分，保持原有行为
        properties.setSegmentSplit(segmentSplit);

        // 创建纯净处理器作为依赖
        PureMarkdownProcessor pureProcessor = new PureMarkdownProcessor(properties);
        enhancedMarkdownProcessor = new EnhancedMarkdownProcessor(Collections.emptyList(), pureProcessor);
    }

    @Test
    void shouldProcessSimpleMarkdown() {
        // Given
        String markdown = "# 标题\n\n这是一段文本。";
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        assertThat(segments.get(0).getContent()).isNotBlank();
    }

    @Test
    void shouldHandleEmptyMarkdown() {
        // Given
        String markdown = "";
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isEmpty();
    }

    @Test
    void shouldHandleTableMarkdown() {
        // Given
        String markdown = """
                # 数据表格

                | 名称 | 年龄 | 职业 |
                |------|------|------|
                | 张三 | 25   | 程序员 |
                | 李四 | 30   | 设计师 |
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        // 验证包含表格相关内容
        boolean hasTableContent = segments.stream()
                .anyMatch(segment -> segment.getContent().contains("张三") || segment.getContent().contains("程序员"));
        assertThat(hasTableContent).isTrue();
    }

    @Test
    void shouldHandleMathFormula() {
        // Given
        String markdown = """
                # 数学公式

                这是一个行内公式：$E = mc^2$

                这是一个块级公式：
                $$\\frac{a}{b} = c$$
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        // 验证包含公式相关内容
        boolean hasFormulaContent = segments.stream()
                .anyMatch(segment -> segment.getContent().contains("E = mc^2") || segment.getContent().contains("公式"));
        assertThat(hasFormulaContent).isTrue();
    }

    @Test
    void shouldHandleImage() {
        // Given
        String markdown = """
                # 图片示例

                ![示例图片](https://example.com/image.jpg)
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        // 验证包含图片的完整markdown语法
        boolean hasImageMarkdown = segments.stream()
                .anyMatch(segment -> segment.getContent().contains("![示例图片](https://example.com/image.jpg)"));
        assertThat(hasImageMarkdown).isTrue();
    }

    @Test
    void shouldHandleBulletList() {
        // Given
        String markdown = """
                # 列表示例

                - 第一项
                - 第二项
                - 第三项
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        // 验证包含列表项标记
        boolean hasListMarkdown = segments.stream()
                .anyMatch(segment -> segment.getContent().contains("- 第一项") && segment.getContent().contains("- 第二项"));
        assertThat(hasListMarkdown).isTrue();
    }

    @Test
    void shouldHandleOrderedList() {
        // Given
        String markdown = """
                # 有序列表示例

                1. 第一步
                2. 第二步
                3. 第三步
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        // 验证包含有序列表标记
        boolean hasOrderedListMarkdown = segments.stream().anyMatch(
                segment -> segment.getContent().contains("1. 第一步") && segment.getContent().contains("2. 第二步"));
        assertThat(hasOrderedListMarkdown).isTrue();
    }

    @Test
    void shouldHandleBlockQuote() {
        // Given
        String markdown = """
                # 引用示例

                > 这是一个引用块
                > 包含多行内容
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();
        // 验证包含引用块标记
        boolean hasBlockQuoteMarkdown = segments.stream()
                .anyMatch(segment -> segment.getContent().contains("> 这是一个引用块"));
        assertThat(hasBlockQuoteMarkdown).isTrue();
    }

    @Test
    void shouldHandleComplexMarkdown() {
        // Given
        String markdown = """
                # 复合结构示例

                这是普通段落。

                ## 图片和列表

                ![架构图](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123559103-f3c118f5-ac99-4f5c-bea9-322aa1fbc95f.png)

                **功能特性：**

                1. 支持图片处理
                2. 支持列表解析
                3. 支持引用块

                **无序列表：**

                - 项目一
                - 项目二

                > 引用：这个功能很重要
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = enhancedMarkdownProcessor.processToSegments(markdown, context);

        // Then
        assertThat(segments).isNotEmpty();

        // 验证图片markdown语法保留
        boolean hasImageMarkdown = segments.stream().anyMatch(segment -> segment.getContent().contains(
                "![架构图](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123559103-f3c118f5-ac99-4f5c-bea9-322aa1fbc95f.png)"));
        assertThat(hasImageMarkdown).isTrue();

        // 验证有序列表保留
        boolean hasOrderedList = segments.stream().anyMatch(segment -> segment.getContent().contains("1. 支持图片处理"));
        assertThat(hasOrderedList).isTrue();

        // 验证无序列表保留
        boolean hasBulletList = segments.stream().anyMatch(segment -> segment.getContent().contains("- 项目一"));
        assertThat(hasBulletList).isTrue();

        // 验证引用块保留
        boolean hasBlockQuote = segments.stream().anyMatch(segment -> segment.getContent().contains("> 引用：这个功能很重要"));
        assertThat(hasBlockQuote).isTrue();
    }
}