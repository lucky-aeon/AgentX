package org.xhy.domain.token.service.impl;

import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.token.model.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.service.TokenOverflowStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 摘要策略Token超限处理实现
 * 将超出阈值的早期消息生成摘要，保留摘要和最新消息
 */
public class SummarizeTokenOverflowStrategy implements TokenOverflowStrategy {

    /**
     * 默认摘要触发阈值（消息数量）
     */
    private static final int DEFAULT_SUMMARY_THRESHOLD = 20;
    
    /**
     * 默认最大Token数
     */
    private static final int DEFAULT_MAX_TOKENS = 4096;
    
    /**
     * 摘要消息的特殊角色标识
     */
    private static final String SUMMARY_ROLE = "summary";
    
    /**
     * 策略配置
     */
    private final TokenOverflowConfig config;
    
    /**
     * 需要进行摘要的消息
     */
    private List<TokenMessage> messagesToSummarize;
    
    /**
     * 生成的摘要消息对象
     */
    private TokenMessage summaryMessage;
    
    /**
     * 构造函数
     * 
     * @param config 策略配置
     */
    public SummarizeTokenOverflowStrategy(TokenOverflowConfig config) {
        this.config = config;
        this.messagesToSummarize = new ArrayList<>();
        this.summaryMessage = null;
    }
    
    /**
     * 处理消息列表，应用摘要策略
     * 将超过阈值的早期消息替换为一个摘要消息
     * 
     * @param messages 待处理的消息列表
     * @return 处理后的消息列表（包含摘要消息+保留的消息）
     */
    @Override
    public TokenProcessResult  process(List<TokenMessage> messages) {
        if (!needsProcessing(messages)) {
            TokenProcessResult result = new TokenProcessResult();
            result.setRetainedMessages(messages);
            result.setStrategyName(getName());
            result.setProcessed(false);
            result.setTotalTokens(calculateTotalTokens(messages));
            return result;
        }

        // 按时间排序
        List<TokenMessage> sortedMessages = new ArrayList<>(messages);
        Collections.sort(sortedMessages, Comparator.comparing(TokenMessage::getCreatedAt));

        // 获取需要保留的消息数量
        int threshold = config.getSummaryThreshold();
        
        // 分割消息：需要摘要的消息和保留的消息
        messagesToSummarize = sortedMessages.subList(0, sortedMessages.size() - threshold);
        List<TokenMessage> retainedMessages = new ArrayList<>(
            sortedMessages.subList(sortedMessages.size() - threshold, sortedMessages.size())
        );

        // 生成摘要消息
        String summary = generateSummary(messagesToSummarize);
        summaryMessage = createSummaryMessage(summary);
        
        // 将摘要消息添加到保留消息列表的开头
        retainedMessages.add(0, summaryMessage);

        // 创建结果对象
        TokenProcessResult result = new TokenProcessResult();
        result.setRetainedMessages(retainedMessages);
        result.setSummary(summary);
        result.setStrategyName(getName());
        result.setProcessed(true);
        result.setTotalTokens(calculateTotalTokens(retainedMessages));
        
        return result;
    }
    
    /**
     * 创建表示摘要的TokenMessage对象
     * 
     * @param summary 摘要内容
     * @return 摘要消息对象
     */
    private TokenMessage createSummaryMessage(String summary) {
        TokenMessage message = new TokenMessage();
        message.setId(UUID.randomUUID().toString());
        message.setRole(SUMMARY_ROLE);
        message.setContent(summary);
        message.setTokenCount(100); // TODO: 计算实际token数
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    @Override
    public String getName() {
        return TokenOverflowStrategyEnum.SUMMARIZE.name();
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
        
        return messages.size() > config.getSummaryThreshold();
    }
    
    /**
     * 获取需要摘要的消息列表（按时间排序）
     * 这是应用层应该使用的方法，用于获取需要进行摘要处理的消息对象
     * 
     * @return 需要摘要的消息列表（按时间从旧到新排序）
     */
    public List<TokenMessage> getMessagesToSummarize() {
        return messagesToSummarize;
    }
    
    /**
     * 生成摘要内容
     */
    private String generateSummary(List<TokenMessage> messages) {
        // TODO: 这里应该调用LLM生成摘要，目前返回简单描述
        return String.format("这里是%d条历史消息的摘要", messages.size());
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
     * 获取生成的摘要消息对象
     * 
     * @return 摘要消息对象
     */
    public TokenMessage getSummaryMessage() {
        return summaryMessage;
    }
} 