package org.xhy.domain.token.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;

/** 滑动窗口策略测试类 */
@SpringBootTest
public class SlidingWindowTokenOverflowStrategyTest {

    private SlidingWindowTokenOverflowStrategy strategy;

    private TokenOverflowConfig config;
    private List<TokenMessage> messages;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        messages = createTestMessages(20, 100);

        // 创建配置
        config = TokenOverflowConfig.createSlidingWindowConfig(1000, 0.1);
        strategy = new SlidingWindowTokenOverflowStrategy(config);
    }

    /** 测试正常处理逻辑 */
    @Test
    public void testProcess() {
        // 执行处理
        TokenProcessResult process = strategy.process(messages, new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();
        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertTrue(result.size() < messages.size(), "应该有消息被移除");

        // 验证总token数
        int totalTokens = result.stream().mapToInt(m -> m.getTokenCount() != null ? m.getTokenCount() : 0).sum();
        assertTrue(totalTokens <= config.getMaxTokens(), "总Token数应该小于等于最大限制");

        // 验证保留的是最新消息
        List<TokenMessage> sortedOriginal = new ArrayList<>(messages);
        Collections.sort(sortedOriginal, Comparator.comparing(TokenMessage::getCreatedAt).reversed());

        for (int i = 0; i < result.size(); i++) {
            assertTrue(result.contains(sortedOriginal.get(i)), "应该保留最新的消息");
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

    /** 测试不需要处理的情况（未超出限制） */
    @Test
    public void testNoProcessingNeeded() {
        // 准备测试数据（总token数为500，未超过限制）
        List<TokenMessage> smallMessages = createTestMessages(5, 100);

        // 创建配置（限制为1000）
        TokenOverflowConfig testConfig = TokenOverflowConfig.createSlidingWindowConfig(1000, 0.1);

        // 执行处理 - 注意：由于测试类自动注入策略，这里需要创建一个新的策略实例
        SlidingWindowTokenOverflowStrategy testStrategy = new SlidingWindowTokenOverflowStrategy(testConfig);

        TokenProcessResult process = testStrategy.process(smallMessages, new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();

        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertEquals(smallMessages.size(), result.size(), "应该保留所有消息");
        assertEquals(smallMessages, result, "应该返回原始列表");
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
