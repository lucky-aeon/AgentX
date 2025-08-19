package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.rag.config.MarkdownProcessorProperties;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证层次化分割算法的核心特性 */
class HierarchicalSplitValidationTest {

    private PureMarkdownProcessor processor;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        // 配置分割参数 - 使用真实场景的参数
        MarkdownProcessorProperties properties = new MarkdownProcessorProperties();
        MarkdownProcessorProperties.SegmentSplit segmentSplit = new MarkdownProcessorProperties.SegmentSplit();
        segmentSplit.setEnabled(true);
        segmentSplit.setMaxLength(1800); // 使用真实的长度限制
        segmentSplit.setMinLength(200);
        segmentSplit.setBufferSize(100);
        properties.setSegmentSplit(segmentSplit);

        processor = new PureMarkdownProcessor(properties);
        context = new ProcessingContext(null, null, null, "testUser", "testFile");
    }

    @Test
    void shouldProcessRealDocument() throws Exception {
        // Given - 读取真实文档
        String docPath = "src/test/java/org/xhy/infrastructure/rag/doc/AgentX 讲义.md";
        String markdown = Files.readString(Paths.get(docPath), StandardCharsets.UTF_8);
        
        System.out.println("=== 处理真实文档：AgentX 讲义.md ===");
        System.out.println("原始文档长度: " + markdown.length() + " 字符");
        
        // When - 处理文档
        List<ProcessedSegment> segments = processor.processToSegments(markdown, context);
        
        // Then - 输出结果供人工检测
        System.out.println("\n=== 处理结果统计 ===");
        System.out.println("生成段落数量: " + segments.size());
        
        int totalLength = segments.stream().mapToInt(s -> s.getContent().length()).sum();
        int maxLength = segments.stream().mapToInt(s -> s.getContent().length()).max().orElse(0);
        int minLength = segments.stream().mapToInt(s -> s.getContent().length()).min().orElse(0);
        double avgLength = segments.isEmpty() ? 0 : (double) totalLength / segments.size();
        
        System.out.printf("段落长度统计: 最长=%d, 最短=%d, 平均=%.1f, 总计=%d%n", 
            maxLength, minLength, avgLength, totalLength);
        
        // 检查是否有超长段落
        long oversizedCount = segments.stream()
            .mapToInt(s -> s.getContent().length())
            .filter(len -> len > 1800)
            .count();
        System.out.println("超过1800字符的段落数量: " + oversizedCount);
        
        // 检查特殊节点
        int totalSpecialNodes = segments.stream()
            .mapToInt(s -> s.hasSpecialNodes() ? s.getSpecialNodes().size() : 0)
            .sum();
        System.out.println("特殊节点总数: " + totalSpecialNodes);
        
        System.out.println("\n=== 前3个段落内容示例 ===");
        for (int i = 0; i < Math.min(3, segments.size()); i++) {
            ProcessedSegment segment = segments.get(i);
            System.out.printf("\n--- 段落 %d (长度: %d) ---\n", i + 1, segment.getContent().length());
            System.out.println(segment.getContent());
        }
        
        System.out.println("\n=== 最后3个段落内容示例 ===");
        int startIndex = Math.max(0, segments.size() - 3);
        for (int i = startIndex; i < segments.size(); i++) {
            ProcessedSegment segment = segments.get(i);
            System.out.printf("\n--- 段落 %d (长度: %d) ---\n", i + 1, segment.getContent().length());
            System.out.println(segment.getContent());
        }
        
        // 验证基本结果
        assertThat(segments).isNotEmpty();
        System.out.println("\n✓ 真实文档处理完成，请检查上述输出结果");
    }
}