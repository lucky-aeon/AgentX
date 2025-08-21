package org.xhy.infrastructure.rag.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 智能二次分割器测试
 * 
 * @author claude */
class SecondarySegmentSplitterTest {

    private SecondarySegmentSplitter splitter;

    @BeforeEach
    void setUp() {
        splitter = new SecondarySegmentSplitter();

        // 设置测试参数
        ReflectionTestUtils.setField(splitter, "maxVectorLength", 200);
        ReflectionTestUtils.setField(splitter, "minVectorLength", 50);
        ReflectionTestUtils.setField(splitter, "overlapSize", 30);
    }

    @Test
    void testSplitIfNeeded_shortContent() {
        String shortContent = "这是一段很短的内容。";

        List<String> result = splitter.splitIfNeeded(shortContent);

        assertEquals(1, result.size());
        assertEquals(shortContent, result.get(0));
    }

    @Test
    void testSplitIfNeeded_withTitleContext() {
        String content = "这是一段很短的内容。";
        String titleContext = "# 测试标题";

        List<String> result = splitter.splitIfNeeded(content, titleContext);

        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith(titleContext));
        assertTrue(result.get(0).contains(content));
    }

    @Test
    void testSplitIfNeeded_longContent() {
        // 创建超长内容
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            longContent.append("这是第").append(i + 1).append("段很长的测试内容，用于测试二次分割功能。");
            longContent.append("内容包含多个句子，以便测试按句子分割的逻辑。");
            longContent.append("每一段都有足够的文字来触发分割机制。\n\n");
        }

        List<String> result = splitter.splitIfNeeded(longContent.toString());

        assertTrue(result.size() > 1, "长内容应该被分割成多个片段");

        // 验证每个片段的长度
        for (String chunk : result) {
            assertTrue(chunk.length() <= 200, "每个片段长度应该不超过最大限制");
        }
    }

    @Test
    void testSplitIfNeeded_paragraphSplit() {
        String content = "第一段内容。这是第一段的详细描述。\n\n" + "第二段内容。这是第二段的详细描述。\n\n" + "第三段内容。这是第三段的详细描述。";

        List<String> result = splitter.splitIfNeeded(content);

        // 验证分割结果
        assertFalse(result.isEmpty());

        // 检查每个块的长度
        for (String chunk : result) {
            assertTrue(chunk.length() <= 200);
        }
    }

    @Test
    void testSplitIfNeeded_sentenceSplit() {
        // 创建包含长句子的内容
        String content = "这是一个很长的句子，包含了大量的文字内容，目的是测试按句子分割的功能。" + "这是第二个长句子，同样包含了大量的文字，用于验证句子分割逻辑。"
                + "这是第三个长句子，确保分割逻辑能够正确处理多个句子的情况。";

        List<String> result = splitter.splitIfNeeded(content);

        assertFalse(result.isEmpty());

        // 验证每个片段的长度
        for (String chunk : result) {
            assertTrue(chunk.length() <= 200);
        }
    }

    @Test
    void testSplitStatistics() {
        String originalContent = "原始内容";
        List<String> chunks = List.of("片段1", "片段2", "片段3");

        String stats = splitter.getSplitStatistics(originalContent, chunks);

        assertNotNull(stats);
        assertTrue(stats.contains("3 chunks"));
        assertTrue(stats.contains(String.valueOf(originalContent.length())));
    }

    @Test
    void testEmptyContent() {
        List<String> result = splitter.splitIfNeeded("");
        assertTrue(result.isEmpty());

        List<String> nullResult = splitter.splitIfNeeded(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    void testContentWithTitlePrefix() {
        String titleContext = "# 测试标题";
        String content = "测试内容";

        // 内容已包含标题上下文的情况
        String contentWithTitle = titleContext + "\n\n" + content;
        List<String> result = splitter.splitIfNeeded(contentWithTitle, titleContext);

        assertEquals(1, result.size());
        assertEquals(contentWithTitle, result.get(0));
    }

    @Test
    void testLongTitleContext() {
        // 测试标题上下文很长的情况
        String longTitle = "# " + "很长的标题".repeat(20);
        String content = "简短内容";

        List<String> result = splitter.splitIfNeeded(content, longTitle);

        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains(content));
    }
}