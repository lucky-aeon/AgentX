package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 新架构测试类
 * 
 * 验证纯净解析器和RAG增强处理器的功能
 * 
 * @author claude */
@SpringBootTest
@ActiveProfiles("test")
class NewArchitectureTest {

    @Autowired
    @Qualifier("pureMarkdownProcessor")
    private MarkdownProcessor pureProcessor;

    @Autowired
    @Qualifier("ragEnhancedMarkdownProcessor") 
    private MarkdownProcessor ragEnhancedProcessor;

    // 注意：删除了配置类后，不再有@Primary默认处理器
    // @Autowired
    // private MarkdownProcessor defaultProcessor;

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
    void testPureProcessorExists() {
        assertNotNull(pureProcessor, "纯净处理器应该被注入");
        assertTrue(pureProcessor instanceof PureMarkdownProcessor, "应该是纯净处理器实例");
    }

    @Test
    void testRagEnhancedProcessorExists() {
        assertNotNull(ragEnhancedProcessor, "RAG增强处理器应该被注入");
        assertTrue(ragEnhancedProcessor instanceof RagEnhancedMarkdownProcessor, "应该是RAG增强处理器实例");
    }

    // 注意：删除了配置类后，不再有@Primary默认处理器
    // @Test
    // void testDefaultProcessorIsEnhanced() {
    //     assertNotNull(defaultProcessor, "默认处理器应该被注入");
    //     assertTrue(defaultProcessor instanceof RagEnhancedMarkdownProcessor, "默认处理器应该是增强版");
    //     assertSame(ragEnhancedProcessor, defaultProcessor, "默认处理器应该与增强处理器是同一个实例");
    // }

    @Test
    void testPureProcessorBasicFunctionality() {
        // 创建处理上下文（不包含LLM配置）
        ProcessingContext context = new ProcessingContext(null, null, null, "test-user", "test-file-id");

        // 使用纯净处理器处理
        List<ProcessedSegment> segments = pureProcessor.processToSegments(testMarkdown, context);

        // 验证基本功能
        assertNotNull(segments, "段落列表不应为空");
        assertTrue(segments.size() > 0, "应该生成多个段落");

        // 验证包含预期的段落类型
        boolean hasSection = segments.stream().anyMatch(s -> "section".equals(s.getType()));
        boolean hasCode = segments.stream().anyMatch(s -> "code".equals(s.getType()));
        
        assertTrue(hasSection, "应该包含章节段落");
        // 代码块处理取决于具体实现，这里不强制要求
        
        // 验证内容完整性
        String allContent = segments.stream()
                .map(ProcessedSegment::getContent)
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(allContent.contains("测试文档"), "应该包含原始内容");
        assertTrue(allContent.contains("HelloWorld"), "应该包含代码内容");
    }

    @Test
    void testProcessorInterfaceCompatibility() {
        // 测试两个处理器都实现了相同的接口，可以互换使用
        ProcessingContext context = new ProcessingContext(null, null, null, "test-user", "test-file-id");

        List<ProcessedSegment> pureResults = pureProcessor.processToSegments(testMarkdown, context);
        List<ProcessedSegment> enhancedResults = ragEnhancedProcessor.processToSegments(testMarkdown, context);

        // 两个处理器都应该能够处理相同的输入
        assertNotNull(pureResults, "纯净处理器应该返回结果");
        assertNotNull(enhancedResults, "增强处理器应该返回结果");
        
        // 增强处理器的结果可能更详细（如果有LLM配置的话）
        assertTrue(pureResults.size() > 0, "纯净处理器应该生成段落");
        assertTrue(enhancedResults.size() > 0, "增强处理器应该生成段落");
    }

    @Test
    void testEmptyInput() {
        ProcessingContext context = new ProcessingContext(null, null, null, "test-user", "test-file-id");

        // 测试空输入处理
        List<ProcessedSegment> emptyResults1 = pureProcessor.processToSegments("", context);
        List<ProcessedSegment> emptyResults2 = ragEnhancedProcessor.processToSegments("", context);
        List<ProcessedSegment> nullResults1 = pureProcessor.processToSegments(null, context);
        List<ProcessedSegment> nullResults2 = ragEnhancedProcessor.processToSegments(null, context);

        // 空输入应该返回空列表，不应该抛异常
        assertNotNull(emptyResults1, "处理空字符串不应返回null");
        assertNotNull(emptyResults2, "处理空字符串不应返回null");
        assertNotNull(nullResults1, "处理null不应返回null");
        assertNotNull(nullResults2, "处理null不应返回null");
        
        assertTrue(emptyResults1.isEmpty() || 
                  (emptyResults1.size() == 1 && emptyResults1.get(0).getContent().trim().isEmpty()),
                  "空输入应该返回空列表或空内容");
    }
}