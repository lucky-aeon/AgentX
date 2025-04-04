package org.xhy.domain.token.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;

import java.util.List;

/**
 * Token领域服务
 * 封装Token超限处理的核心逻辑
 */
@Service
public class TokenDomainService {

    private final TokenOverflowStrategyFactory strategyFactory;
    
    public TokenDomainService(TokenOverflowStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    /**
     * 处理消息列表
     *
     * @param messages 待处理的消息列表
     * @param config 处理配置
     * @return 处理结果
     */
    public TokenProcessResult processMessages(List<TokenMessage> messages, TokenOverflowConfig config) {
        // 创建策略
        TokenOverflowStrategy strategy = strategyFactory.createStrategy(config);
        
        // 执行处理
        return strategy.process(messages,config);
    }
    
    /**
     * 计算消息列表的总Token数
     *
     * @param messages 消息列表
     * @return 总Token数
     */
    private int calculateTotalTokens(List<TokenMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        return messages.stream()
                .mapToInt(message -> {
                    Integer tokenCount = message.getTokenCount();
                    return tokenCount != null ? tokenCount : 0;
                })
                .sum();
    }
} 