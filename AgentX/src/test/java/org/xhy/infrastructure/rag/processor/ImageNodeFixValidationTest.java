package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 图片节点修复验证测试 */
class ImageNodeFixValidationTest {

    private PureMarkdownProcessor pureProcessor;

    @BeforeEach
    void setUp() {
        pureProcessor = new PureMarkdownProcessor();
    }

    @Test
    void shouldPreserveImageLinksInRealDocument() throws IOException {
        // Given - 读取真实的AgentX讲义文件
        String testDocPath = "/Users/xhy/course/AgentX/AgentX/src/test/java/org/xhy/infrastructure/rag/doc/AgentX 讲义.md";
        String markdown = Files.readString(Path.of(testDocPath));
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When - 处理markdown
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then - 验证图片链接被正确保留
        System.out.println("总共生成了 " + segments.size() + " 个段落");

        // 检查是否包含架构设计图
        boolean hasArchitectureDiagram = segments.stream()
                .anyMatch(segment -> {
                    String content = segment.getContent();
                    boolean contains = content.contains("![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123559103-f3c118f5-ac99-4f5c-bea9-322aa1fbc95f.png)");
                    if (contains) {
                        System.out.println("找到架构设计图片链接！");
                        System.out.println("段落内容片段: " + content.substring(Math.max(0, content.indexOf("架构设计图") - 20), 
                                                                Math.min(content.length(), content.indexOf("架构设计图") + 200)));
                    }
                    return contains;
                });

        // 检查是否包含流程图
        boolean hasFlowChart = segments.stream()
                .anyMatch(segment -> {
                    String content = segment.getContent();
                    boolean contains = content.contains("![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123580936-c9c1c769-5c47-4915-baa8-a8683b22af05.png)");
                    if (contains) {
                        System.out.println("找到流程图片链接！");
                        System.out.println("段落内容片段: " + content.substring(Math.max(0, content.indexOf("流程图") - 20), 
                                                                Math.min(content.length(), content.indexOf("流程图") + 200)));
                    }
                    return contains;
                });

        // 统计所有图片链接
        long imageCount = segments.stream()
                .mapToLong(segment -> {
                    String content = segment.getContent();
                    return content.split("!\\[.*?\\]\\(https://cdn\\.nlark\\.com/.*?\\)").length - 1;
                })
                .sum();

        System.out.println("检测到的图片链接总数: " + imageCount);

        // 验证关键图片链接存在
        assertThat(hasArchitectureDiagram).as("应该包含架构设计图").isTrue();
        assertThat(hasFlowChart).as("应该包含流程图").isTrue();
        assertThat(imageCount).as("应该包含多个图片链接").isGreaterThan(5);
    }

    @Test
    void shouldPreserveVariousImageFormats() {
        // Given - 包含各种图片格式的markdown
        String markdown = """
                # 图片测试
                
                ## 架构设计图
                
                ![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123559103-f3c118f5-ac99-4f5c-bea9-322aa1fbc95f.png)
                
                **流程图**
                
                ![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123580936-c9c1c769-5c47-4915-baa8-a8683b22af05.png)
                
                状态机：A->B->C->D
                ![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1747755486684-36919bc0-cab7-437b-a3c9-1884d5c65199.png)
                """;
        ProcessingContext context = new ProcessingContext(null, null, null, "testUser", "testFile");

        // When
        List<ProcessedSegment> segments = pureProcessor.processToSegments(markdown, context);

        // Then - 验证所有图片都被保留
        System.out.println("\n=== 处理后的段落内容 ===");
        for (int i = 0; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            System.out.println("段落 " + (i + 1) + " (类型: " + segment.getType() + "):");
            System.out.println(segment.getContent());
            System.out.println("---");
        }

        // 验证图片链接保留
        boolean hasAllImages = segments.stream()
                .anyMatch(segment -> {
                    String content = segment.getContent();
                    return content.contains("1754123559103-f3c118f5-ac99-4f5c-bea9-322aa1fbc95f.png") &&
                           content.contains("1754123580936-c9c1c769-5c47-4915-baa8-a8683b22af05.png") &&
                           content.contains("1747755486684-36919bc0-cab7-437b-a3c9-1884d5c65199.png");
                });

        assertThat(hasAllImages).isTrue();
    }
}