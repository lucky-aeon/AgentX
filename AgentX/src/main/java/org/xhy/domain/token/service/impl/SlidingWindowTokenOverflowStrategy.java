package org.xhy.domain.token.service.impl;

import org.springframework.stereotype.Service;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.service.TokenOverflowStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 滑动窗口Token超限处理策略实现
 * 根据Token数量保留最新消息，超出窗口的旧消息将被丢弃
 */
@Service
public class SlidingWindowTokenOverflowStrategy implements TokenOverflowStrategy {

    /**
     * 默认最大Token数
     */
    private static final int DEFAULT_MAX_TOKENS = 4096;
    
    /**
     * 默认预留缓冲比例
     */
    private static final double DEFAULT_RESERVE_RATIO = 0.1;
    
    /**
     * 策略配置
     */
    private final TokenOverflowConfig config;
    
    /**
     * 构造函数
     * 
     * @param config 策略配置
     */
    public SlidingWindowTokenOverflowStrategy(TokenOverflowConfig config) {
        this.config = config;
    }
    
    /**
     * 处理消息列表，应用滑动窗口策略
     * 
     * @param messages 待处理的消息列表
     * @return 处理后保留的消息列表
     */
    @Override
    public TokenProcessResult process(List<TokenMessage> messages,TokenOverflowConfig tokenOverflowConfig) {
        if (!needsProcessing(messages)) {
            TokenProcessResult result = new TokenProcessResult();
            result.setRetainedMessages(messages);
            result.setStrategyName(getName());
            result.setProcessed(false);
            result.setTotalTokens(calculateTotalTokens(messages));
            return result;
        }

        // 按时间排序，保留最新的消息
        List<TokenMessage> sortedMessages = new ArrayList<>(messages);
        Collections.sort(sortedMessages, Comparator.comparing(TokenMessage::getCreatedAt).reversed());

        // 计算可用token数（考虑预留空间）
        int maxTokens = config.getMaxTokens();

        // 保留最新的消息，直到达到token限制
        List<TokenMessage> retainedMessages = new ArrayList<>();
        int totalTokens = 0;

        for (TokenMessage message : sortedMessages) {
            int messageTokens = message.getBodyTokenCount() != null ? message.getBodyTokenCount() : 0;
            if (totalTokens + messageTokens <= maxTokens) {
                retainedMessages.add(message);
                totalTokens += messageTokens;
            } else {
                break;
            }
        }

        // 创建结果对象
        TokenProcessResult result = new TokenProcessResult();
        result.setRetainedMessages(retainedMessages);
        result.setStrategyName(getName());
        result.setProcessed(true);
        result.setTotalTokens(totalTokens);
        
        return result;
    }
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    @Override
    public String getName() {
        return TokenOverflowStrategyEnum.SLIDING_WINDOW.name();
    }
    
    /**
     * 判断是否需要进行Token超限处理
     * 
     * @param messages 待处理的消息列表
     * @return 是否需要处理
     */
    @Override
    public boolean needsProcessing(List<TokenMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        
        int totalTokens = calculateTotalTokens(messages);
        int maxTokens = config.getMaxTokens();
        return totalTokens > maxTokens;
    }
    

    /**
     * 计算消息列表的总token数
     */
    private int calculateTotalTokens(List<TokenMessage> messages) {
        return messages.stream()
                .mapToInt(m -> m.getTokenCount() != null ? m.getTokenCount() : 0)
                .sum();
    }
    
    /**
     * 获取配置的最大Token数，如果未配置则使用默认值
     * 
     * @return 最大Token数
     */
    private int getMaxTokens() {
        if (config == null || config.getMaxTokens() == null) {
            return DEFAULT_MAX_TOKENS;
        }
        return config.getMaxTokens();
    }
    
    /**
     * 获取配置的预留比例，如果未配置则使用默认值
     * 
     * @return 预留比例
     */
    private double getReserveRatio() {
        if (config == null || config.getReserveRatio() == null) {
            return DEFAULT_RESERVE_RATIO;
        }
        return config.getReserveRatio();
    }
} 