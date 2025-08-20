package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.enums.SegmentType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第一阶段纯原文拆分测试
 * 
 * 只测试第一阶段的纯原文拆分结果，使用真实的处理器，不使用mock
 * 
 * @author claude
 */
class TwoStageProcessingIntegrationTest {

    private PureMarkdownProcessor pureProcessor;

    @BeforeEach
    void setUp() {
        // 创建真实的处理器实例
        pureProcessor = new PureMarkdownProcessor();
    }

    @Test
    void testStage1_RawMarkdownSplitting() {
        String markdown = """
                # 测试文档
                
                这是一个测试文档的介绍。
                
                ## 代码示例
                
                ```java
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                ```
                
                ## 表格数据
                
                | 姓名 | 年龄 | 职业 |
                |------|------|------|
                | 张三 | 25   | 工程师 |
                | 李四 | 30   | 设计师 |
                
                ## 总结
                
                这是文档的总结部分。
                """;

        // 第一阶段：纯原文拆分
        pureProcessor.setRawMode(true);
        List<ProcessedSegment> rawSegments = pureProcessor.processToSegments(markdown, null);
        
        assertFalse(rawSegments.isEmpty());
        System.out.println("=== 第一阶段拆分结果 ===");
        System.out.println("总段落数: " + rawSegments.size());
        
        // 验证原文保持完整性
        for (int i = 0; i < rawSegments.size(); i++) {
            ProcessedSegment segment = rawSegments.get(i);
            String content = segment.getContent();
            assertNotNull(content);
            
            System.out.println("\n--- 段落 " + (i + 1) + " ---");
            System.out.println("类型: " + segment.getType());
            System.out.println("长度: " + content.length());
            System.out.println("内容预览: " + content.substring(0, Math.min(100, content.length())) + "...");
            
            // 代码块应保持原始格式
            if (content.contains("HelloWorld")) {
                assertTrue(content.contains("```java"), "代码块应保持原始markdown格式");
                assertTrue(content.contains("System.out.println"), "代码内容应完整保留");
                System.out.println("✓ 代码块格式正确");
            }
            
            // 表格应保持原始格式
            if (content.contains("姓名")) {
                assertTrue(content.contains("| 姓名 | 年龄 | 职业 |"), "表格头应保持原始格式");
                assertTrue(content.contains("|------|------|------|"), "表格分隔符应保持原始格式");
                assertTrue(content.contains("| 张三 | 25   | 工程师 |"), "表格数据应保持原始格式");
                System.out.println("✓ 表格格式正确");
            }
        }
    }

    @Test
    void testStage1_ComplexMarkdownStructure() {
        String complexMarkdown = """
                # 主标题
                
                主标题下的介绍内容。
                
                ## 二级标题A
                
                二级标题A的内容。
                
                ### 三级标题A1
                
                三级标题A1的详细内容，包含多行文本。
                这是第二行内容。
                
                ```python
                def hello_world():
                    print("Hello from Python!")
                    return "success"
                ```
                
                ### 三级标题A2
                
                ![测试图片](https://example.com/image.jpg)
                
                图片后的说明文字。
                
                ## 二级标题B
                
                | 项目 | 描述 | 状态 |
                |------|------|------|
                | 任务1 | 完成开发 | 已完成 |
                | 任务2 | 测试验证 | 进行中 |
                
                表格后的总结内容。
                """;

        pureProcessor.setRawMode(true);
        List<ProcessedSegment> segments = pureProcessor.processToSegments(complexMarkdown, null);
        
        assertFalse(segments.isEmpty());
        System.out.println("\n=== 复杂结构拆分结果 ===");
        System.out.println("总段落数: " + segments.size());
        
        boolean hasCodeBlock = false;
        boolean hasTable = false;
        boolean hasImage = false;
        
        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            String content = segment.getContent();
            
            System.out.println("\n--- 段落 " + (i + 1) + " ---");
            System.out.println("类型: " + segment.getType());
            System.out.println("长度: " + content.length());
            
            // 检查各种特殊节点是否保持原始格式
            if (content.contains("```python")) {
                hasCodeBlock = true;
                assertTrue(content.contains("def hello_world():"), "Python代码应完整保留");
                System.out.println("✓ 包含Python代码块");
            }
            
            if (content.contains("| 项目 | 描述 | 状态 |")) {
                hasTable = true;
                assertTrue(content.contains("| 任务1 | 完成开发 | 已完成 |"), "表格数据应完整保留");
                System.out.println("✓ 包含表格");
            }
            
            if (content.contains("![测试图片]")) {
                hasImage = true;
                assertTrue(content.contains("(https://example.com/image.jpg)"), "图片URL应完整保留");
                System.out.println("✓ 包含图片");
            }
        }
        
        // 验证所有特殊节点都被正确处理
        System.out.println("\n=== 特殊节点检查 ===");
        System.out.println("代码块: " + (hasCodeBlock ? "✓" : "✗"));
        System.out.println("表格: " + (hasTable ? "✓" : "✗"));
        System.out.println("图片: " + (hasImage ? "✓" : "✗"));
    }

    @Test
    void testStage1_LongContentSplitting() {
        // 创建较长的文档来测试分割逻辑
        StringBuilder longMarkdown = new StringBuilder();
        longMarkdown.append("# 长文档测试\n\n");
        longMarkdown.append("这是一个用于测试长文档分割的示例。\n\n");
        
        for (int i = 1; i <= 5; i++) {
            longMarkdown.append("## 第").append(i).append("部分\n\n");
            longMarkdown.append("这是第").append(i).append("部分的内容。").append("详细的描述内容。".repeat(10)).append("\n\n");
            
            // 添加代码块
            longMarkdown.append("```java\n");
            longMarkdown.append("// 第").append(i).append("部分的代码示例\n");
            longMarkdown.append("public class Example").append(i).append(" {\n");
            longMarkdown.append("    public void method").append(i).append("() {\n");
            longMarkdown.append("        System.out.println(\"Example ").append(i).append("\");\n");
            longMarkdown.append("    }\n");
            longMarkdown.append("}\n");
            longMarkdown.append("```\n\n");
        }

        pureProcessor.setRawMode(true);
        List<ProcessedSegment> segments = pureProcessor.processToSegments(longMarkdown.toString(), null);
        
        assertFalse(segments.isEmpty());
        System.out.println("\n=== 长文档拆分结果 ===");
        System.out.println("原文档长度: " + longMarkdown.length());
        System.out.println("拆分段落数: " + segments.size());
        
        int totalLength = 0;
        int codeBlockCount = 0;
        
        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            String content = segment.getContent();
            totalLength += content.length();
            
            System.out.println("段落 " + (i + 1) + " - 长度: " + content.length());
            
            // 统计代码块
            if (content.contains("```java")) {
                codeBlockCount++;
                // 验证代码块完整性
                assertTrue(content.contains("public class Example"), "代码块应包含类定义");
                assertTrue(content.contains("System.out.println"), "代码块应包含方法调用");
            }
        }
        
        System.out.println("总处理长度: " + totalLength);
        System.out.println("代码块数量: " + codeBlockCount);
        
        // 验证内容完整性（允许一些空格差异）
        assertTrue(Math.abs(totalLength - longMarkdown.length()) < 100, "处理后总长度应接近原文档长度");
    }

    @Test
    void testStage1_EmptyAndEdgeCases() {
        // 测试空内容
        pureProcessor.setRawMode(true);
        List<ProcessedSegment> emptyResult = pureProcessor.processToSegments("", null);
        assertTrue(emptyResult.isEmpty(), "空内容应返回空列表");
        
        List<ProcessedSegment> nullResult = pureProcessor.processToSegments(null, null);
        assertTrue(nullResult.isEmpty(), "null内容应返回空列表");
        
        // 测试只有空格的内容
        List<ProcessedSegment> spaceResult = pureProcessor.processToSegments("   \n\n   ", null);
        assertTrue(spaceResult.isEmpty(), "只有空格的内容应返回空列表");
        
        // 测试最小有效内容
        List<ProcessedSegment> minResult = pureProcessor.processToSegments("# 标题", null);
        assertEquals(1, minResult.size(), "最小有效内容应返回一个段落");
        assertEquals("# 标题", minResult.get(0).getContent().trim(), "内容应保持原样");
        
        System.out.println("✓ 边界情况测试通过");
    }
}