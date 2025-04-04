package org.xhy.domain.token.service.impl;

import org.springframework.stereotype.Service;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.service.TokenOverflowStrategy;

import java.util.List;

/**
 * 无策略的Token超限处理实现
 * 不对消息进行任何处理，保留所有消息
 */
@Service
public class NoTokenOverflowStrategy implements TokenOverflowStrategy {

    /**
     * 策略配置，无策略实现不使用配置参数，但保留字段以统一接口
     */
    private final TokenOverflowConfig config;
    
    /**
     * 构造函数
     */
    public NoTokenOverflowStrategy() {
        this.config = TokenOverflowConfig.createDefault();
    }
    
    /**
     * 带配置的构造函数
     * 
     * @param config 策略配置
     */
    public NoTokenOverflowStrategy(TokenOverflowConfig config) {
        this.config = config != null ? config : TokenOverflowConfig.createDefault();
    }
    
    /**
     * 处理消息列表，无策略实现不做任何处理，返回原消息列表
     *
     * @param messages 待处理的消息列表
     * @return 原消息列表，不做修改
     */
    @Override
    public TokenProcessResult process(List<TokenMessage> messages,TokenOverflowConfig tokenOverflowConfig) {
        TokenProcessResult result = new TokenProcessResult();
        result.setRetainedMessages(messages);
        result.setStrategyName(getName());
        result.setProcessed(false);
        result.setTotalTokens(calculateTotalTokens(messages));
        return result;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String getName() {
        return TokenOverflowStrategyEnum.NONE.name();
    }

    /**
     * 是否需要进行Token超限处理
     * 无策略实现始终返回false，表示不需要处理
     *
     * @param messages 待处理的消息列表
     * @return 始终返回false，表示不处理
     */
    @Override
    public boolean needsProcessing(List<TokenMessage> messages) {
        // 无策略实现，始终返回false，表示不需要处理
        return false;
    }
    
    /**
     * 计算消息列表的总token数
     */
    private int calculateTotalTokens(List<TokenMessage> messages) {
        return messages.stream()
                .mapToInt(m -> m.getTokenCount() != null ? m.getTokenCount() : 0)
                .sum();
    }
} 