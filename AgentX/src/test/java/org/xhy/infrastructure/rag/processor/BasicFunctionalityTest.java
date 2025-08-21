package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 基础功能测试
 * 
 * 不依赖Spring Context，直接测试新架构的核心功能
 * 
 * @author claude */
class BasicFunctionalityTest {

    private final String testMarkdown = """
            # 测试文档

            这是一个测试文档。

            ## 代码示例

            ```java
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello World!");
                }
            }
            ```

            ## 表格示例

            | 姓名 | 年龄 | 城市 |
            |------|------|------|
            | 张三 | 25   | 北京 |
            | 李四 | 30   | 上海 |
            """;

    @Test
    void testPureMarkdownProcessorDirectly() {
        // 直接创建纯净处理器实例
        MarkdownProcessor pureProcessor = new PureMarkdownProcessor();

        // 创建处理上下文（不包含LLM配置）
        ProcessingContext context = new ProcessingContext(null, null, null, "test-user", "test-file-id");

        // 使用纯净处理器处理
        List<ProcessedSegment> segments = pureProcessor.processToSegments(testMarkdown, context);

        // 验证基本功能
        assertNotNull(segments, "段落列表不应为空");
        assertTrue(segments.size() > 0, "应该生成多个段落");

        // 验证包含预期的段落类型
        boolean hasSection = segments.stream()
                .anyMatch(s -> org.xhy.domain.rag.model.enums.SegmentType.SECTION.equals(s.getType()));
        assertTrue(hasSection, "应该包含章节段落");

        // 验证内容完整性
        String allContent = segments.stream().map(ProcessedSegment::getContent).reduce("", (a, b) -> a + "\n" + b);
        assertTrue(allContent.contains("测试文档"), "应该包含原始内容");
        // 在新架构中，代码块内容应该被正确保留
        assertTrue(allContent.contains("HelloWorld") || allContent.contains("public class"), "应该包含代码内容");

        System.out.println("纯净处理器测试通过！生成了 " + segments.size() + " 个段落");
        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            System.out.println("段落 " + (i + 1) + " (类型: " + segment.getType() + "): "
                    + segment.getContent().substring(0, Math.min(50, segment.getContent().length())) + "...");
        }
    }

    @Test
    void testEmptyInput() {
        MarkdownProcessor pureProcessor = new PureMarkdownProcessor();
        ProcessingContext context = new ProcessingContext(null, null, null, "test-user", "test-file-id");

        // 测试空输入处理
        List<ProcessedSegment> emptyResults = pureProcessor.processToSegments("", context);
        List<ProcessedSegment> nullResults = pureProcessor.processToSegments(null, context);

        // 空输入应该返回空列表，不应该抛异常
        assertNotNull(emptyResults, "处理空字符串不应返回null");
        assertNotNull(nullResults, "处理null不应返回null");

        System.out.println("空输入测试通过！");
    }

    @Test
    void testMarkdownProcessorInterface() {
        // 测试接口兼容性
        MarkdownProcessor processor = new PureMarkdownProcessor();

        assertNotNull(processor, "应该能创建MarkdownProcessor实例");

        ProcessingContext context = new ProcessingContext(null, null, null, "test-user", "test-file-id");
        List<ProcessedSegment> result = processor.processToSegments("# 简单测试", context);

        assertNotNull(result, "应该返回结果");
        assertTrue(result.size() > 0, "应该处理简单markdown");

        System.out.println("接口兼容性测试通过！");
    }
}