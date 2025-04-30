package org.xhy.domain.token.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;

/** 无策略测试类 */
@SpringBootTest
public class NoTokenOverflowStrategyTest {

    private NoTokenOverflowStrategy strategy;
    private TokenOverflowConfig config;
    private List<TokenMessage> messages;

    @BeforeEach
    public void setUp() {
        // 初始化配置
        config = new TokenOverflowConfig();
        config.setStrategyType(TokenOverflowStrategyEnum.NONE);

        // 初始化策略
        strategy = new NoTokenOverflowStrategy(config);

        // 准备测试数据
        messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TokenMessage message = new TokenMessage();
            message.setId(UUID.randomUUID().toString());
            message.setRole(i % 2 == 0 ? "user" : "assistant");
            message.setContent("测试消息 " + i);
            message.setTokenCount(100); // 每条消息100个token
            message.setCreatedAt(LocalDateTime.now().minusMinutes(10 - i)); // 按时间顺序
            messages.add(message);
        }
    }

    @Test
    public void process_shouldReturnOriginalMessages() {
        // 执行处理
        TokenProcessResult process = strategy.process(messages, new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();

        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertEquals(messages.size(), result.size(), "结果大小应该与原始消息相同");
        assertEquals(messages, result, "结果应该与原始消息完全相同");
    }

    @Test
    public void process_withEmptyList_shouldReturnEmptyList() {
        // 执行处理
        TokenProcessResult process = strategy.process(new ArrayList<>(), new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void process_withNullList_shouldReturnEmptyList() {
        // 执行处理
        TokenProcessResult process = strategy.process(null, new TokenOverflowConfig());
        List<TokenMessage> result = process.getRetainedMessages();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getName_shouldReturnCorrectName() {
        // 执行方法
        String name = strategy.getName();

        // 验证结果
        assertEquals(TokenOverflowStrategyEnum.NONE.name(), name);
    }

    @Test
    public void needsProcessing_shouldAlwaysReturnFalse() {
        // 对各种情况进行测试
        assertFalse(strategy.needsProcessing(messages));
        assertFalse(strategy.needsProcessing(new ArrayList<>()));
        assertFalse(strategy.needsProcessing(null));
    }
}
