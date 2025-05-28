package org.xhy.domain.token.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 摘要策略测试类 */
@SpringBootTest
public class SummarizeTokenOverflowStrategyTest {

    private SummarizeTokenOverflowStrategy strategy;

    private TokenOverflowConfig config;
    private List<TokenMessage> messages;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        messages = createTestMessages(30, 100);

        // 创建配置（超过20条触发摘要）
        config = TokenOverflowConfig.createSummaryConfig(4096, 20);
        strategy = new SummarizeTokenOverflowStrategy(config);
    }

    /** 测试正常处理逻辑 */
    @Test
    public void testProcess() {
        // 执行处理
        TokenProcessResult process = strategy.process(messages, new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();

        // 验证结果
        assertNotNull(result, "结果不应为空");

        // 消息数量应该是 原始消息数量减去超过阈值的消息数量 再加上1条摘要消息
        int expectedSize = config.getSummaryThreshold() + 1; // 阈值 + 摘要
        assertEquals(expectedSize, result.size(), "结果应该包含阈值数量的消息+1条摘要消息");

        // 验证摘要消息
        boolean hasSummaryMessage = result.stream().anyMatch(m -> "summary".equals(m.getRole()));
        assertTrue(hasSummaryMessage, "结果应该包含摘要消息");

        // 验证是否正确保留了最新消息
        List<TokenMessage> sortedOriginal = new ArrayList<>(messages);
        Collections.sort(sortedOriginal, Comparator.comparing(TokenMessage::getCreatedAt));
        List<TokenMessage> latestMessages = sortedOriginal.subList(sortedOriginal.size() - config.getSummaryThreshold(),
                sortedOriginal.size());

        for (TokenMessage message : latestMessages) {
            assertTrue(
                    result.stream().anyMatch(m -> m.getId().equals(message.getId()) && !m.getRole().equals("summary")),
                    "结果应该包含最新的原始消息");
        }
    }

    /** 测试空消息列表的处理 */
    @Test
    public void testProcessWithEmptyList() {
        // 执行处理
        TokenProcessResult process = strategy.process(new ArrayList<>(), new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();
        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertTrue(result.isEmpty(), "结果应该是空列表");
    }

    /** 测试不需要处理的情况（未超出阈值） */
    @Test
    public void testNoProcessingNeeded() {
        // 准备测试数据（只有10条消息，未超过阈值）
        List<TokenMessage> smallMessages = createTestMessages(10, 100);

        // 创建配置（超过20条触发摘要）
        TokenOverflowConfig testConfig = TokenOverflowConfig.createSummaryConfig(4096, 20);

        // 执行处理 - 注意：由于测试类自动注入策略，这里需要创建一个新的策略实例
        SummarizeTokenOverflowStrategy testStrategy = new SummarizeTokenOverflowStrategy(testConfig);
        TokenProcessResult process = testStrategy.process(smallMessages, new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();
        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertEquals(smallMessages.size(), result.size(), "应该保留所有消息");
        assertEquals(smallMessages, result, "应该返回原始列表");
    }

    /** 测试GetMessagesToSummarize方法 */
    @Test
    public void testGetMessagesToSummarize() {
        // 先执行处理
        strategy.process(messages, new TokenOverflowConfig());

        // 获取需要摘要的消息
        List<TokenMessage> messagesToSummarize = strategy.getMessagesToSummarize();

        // 验证结果
        assertNotNull(messagesToSummarize, "需要摘要的消息列表不应为空");
        assertEquals(10, messagesToSummarize.size(), "应该有10条消息需要摘要");

        // 验证是否是按时间顺序排序的
        for (int i = 1; i < messagesToSummarize.size(); i++) {
            assertFalse(
                    messagesToSummarize.get(i - 1).getCreatedAt().isAfter(messagesToSummarize.get(i).getCreatedAt()),
                    "需要摘要的消息应该按时间顺序排序");
        }
    }

    /** 创建指定数量和token数的测试消息 */
    private List<TokenMessage> createTestMessages(int count, int tokensPerMessage) {
        List<TokenMessage> messages = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            TokenMessage message = new TokenMessage();
            message.setId(UUID.randomUUID().toString());
            message.setRole(i % 2 == 0 ? "user" : "assistant");
            message.setContent("测试消息 " + i);
            message.setTokenCount(tokensPerMessage);
            message.setCreatedAt(LocalDateTime.now().minusMinutes(count - i)); // 按时间顺序
            messages.add(message);
        }

        return messages;
    }
}