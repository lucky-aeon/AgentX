package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PureMarkdownProcessor 纯原文模式测试
 * 
 * @author claude
 */
class PureMarkdownProcessorRawModeTest {

    private PureMarkdownProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PureMarkdownProcessor();
    }

    @Test
    void testRawModeFlag() {
        // 测试设置和获取原文模式
        assertFalse(processor.isRawMode());
        
        processor.setRawMode(true);
        assertTrue(processor.isRawMode());
        
        processor.setRawMode(false);
        assertFalse(processor.isRawMode());
    }

    @Test
    void testProcessInRawMode() {
        String markdown = """
                # 第一级标题
                
                这是第一级标题下的内容。
                
                ## 第二级标题
                
                这是第二级标题下的内容。
                
                ```java
                System.out.println("Hello World");
                ```
                
                ### 第三级标题
                
                这是第三级标题下的内容。
                """;

        // 启用原文模式
        processor.setRawMode(true);
        
        List<ProcessedSegment> segments = processor.processToSegments(markdown, null);
        
        assertFalse(segments.isEmpty());
        
        // 验证原文模式：内容应保持原始格式
        for (ProcessedSegment segment : segments) {
            assertNotNull(segment.getContent());
            assertNotNull(segment.getType());
            
            // 在原文模式下，代码块等特殊节点应保持原始格式
            String content = segment.getContent();
            if (content.contains("System.out.println")) {
                assertTrue(content.contains("```java"), "代码块应保持原始markdown格式");
            }
        }
    }

    @Test
    void testProcessInPlaceholderMode() {
        String markdown = """
                # 测试标题
                
                正常段落文本。
                
                ```python
                print("Hello World")
                ```
                
                更多文本内容。
                """;

        // 使用占位符模式（默认模式）
        processor.setRawMode(false);
        
        List<ProcessedSegment> segments = processor.processToSegments(markdown, null);
        
        assertFalse(segments.isEmpty());
        
        // 在占位符模式下，可能会生成占位符
        boolean hasCodeContent = false;
        for (ProcessedSegment segment : segments) {
            if (segment.getContent().contains("print") || 
                segment.getContent().contains("{{CODE_")) {
                hasCodeContent = true;
                break;
            }
        }
        assertTrue(hasCodeContent, "应该包含代码相关内容或占位符");
    }

    @Test
    void testRawModeWithComplexMarkdown() {
        String complexMarkdown = """
                # 主标题
                
                介绍段落。
                
                ## 代码示例
                
                ```java
                public class Example {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
                ```
                
                ## 表格示例
                
                | 列1 | 列2 | 列3 |
                |-----|-----|-----|
                | A   | B   | C   |
                | 1   | 2   | 3   |
                
                ## 图片示例
                
                ![测试图片](http://example.com/image.jpg)
                
                结束段落。
                """;

        processor.setRawMode(true);
        
        List<ProcessedSegment> segments = processor.processToSegments(complexMarkdown, null);
        
        assertFalse(segments.isEmpty());
        
        // 验证特殊节点保持原始格式
        String allContent = segments.stream()
                .map(ProcessedSegment::getContent)
                .reduce("", (a, b) -> a + "\n" + b);
        
        assertTrue(allContent.contains("```java"), "应保持代码块原始格式");
        assertTrue(allContent.contains("| 列1 | 列2 | 列3 |"), "应保持表格原始格式");
        assertTrue(allContent.contains("![测试图片]"), "应保持图片原始格式");
    }

    @Test
    void testEmptyContent() {
        processor.setRawMode(true);
        
        List<ProcessedSegment> segments = processor.processToSegments("", null);
        assertTrue(segments.isEmpty());
        
        segments = processor.processToSegments(null, null);
        assertTrue(segments.isEmpty());
        
        segments = processor.processToSegments("   ", null);
        assertTrue(segments.isEmpty());
    }

    @Test
    void testModeConsistency() {
        String testMarkdown = """
                # 测试
                
                内容段落。
                
                ```code
                test code
                ```
                """;

        // 测试模式切换的一致性
        processor.setRawMode(true);
        List<ProcessedSegment> rawSegments = processor.processToSegments(testMarkdown, null);
        
        processor.setRawMode(false);
        List<ProcessedSegment> placeholderSegments = processor.processToSegments(testMarkdown, null);
        
        // 两种模式应该都能正常处理
        assertFalse(rawSegments.isEmpty());
        assertFalse(placeholderSegments.isEmpty());
        
        // 验证模式状态正确
        assertFalse(processor.isRawMode());
    }

    @Test
    void testSegmentOrdering() {
        String markdown = """
                # 第一部分
                内容1
                
                # 第二部分  
                内容2
                
                # 第三部分
                内容3
                """;

        processor.setRawMode(true);
        
        List<ProcessedSegment> segments = processor.processToSegments(markdown, null);
        
        // 验证段落顺序
        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            assertEquals(i, segment.getOrder(), "段落顺序应该正确");
        }
    }

    @Test
    void testErrorHandling() {
        processor.setRawMode(true);
        
        // 测试包含特殊字符的markdown
        String problematicMarkdown = "# 标题\n\n内容\u0000包含null字符";
        
        // 应该能够处理而不抛出异常
        assertDoesNotThrow(() -> {
            List<ProcessedSegment> segments = processor.processToSegments(problematicMarkdown, null);
            assertFalse(segments.isEmpty());
        });
    }
}