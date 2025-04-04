package org.xhy.domain.conversation.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.token.service.TokenDomainService;
import org.xhy.infrastructure.llm.config.ProviderConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文处理器
 * 负责处理对话上下文和消息列表的相关业务逻辑
 */
@Service
public class ContextProcessor {

    private final ContextDomainService contextDomainService;
    private final MessageDomainService messageDomainService;
    private final TokenDomainService tokenDomainService;

    public ContextProcessor(
            ContextDomainService contextDomainService,
            MessageDomainService messageDomainService,
            TokenDomainService tokenDomainService) {
        this.contextDomainService = contextDomainService;
        this.messageDomainService = messageDomainService;
        this.tokenDomainService = tokenDomainService;
    }

    /**
     * 处理会话上下文和消息列表
     *
     * @param sessionId       会话ID
     * @param maxTokens       最大token数
     * @param strategyType    策略类型
     * @param summaryThreshold 摘要阈值
     * @param providerConfig  提供商配置
     * @return 处理后的上下文和消息信息
     */
    public ContextResult processContext(
            String sessionId,
            int maxTokens,
            TokenOverflowStrategyEnum strategyType,
            int summaryThreshold,
            ProviderConfig providerConfig) {

        ContextEntity contextEntity = contextDomainService.findBySessionId(sessionId);
        List<MessageEntity> messageEntities = new ArrayList<>();

        if (contextEntity != null) {
            // 根据消息上下文获取消息列表
            List<String> activeMessagesIds = contextEntity.getActiveMessages();
            messageEntities = messageDomainService.listByIds(activeMessagesIds);

            // 尝试触发 token 策略
            List<TokenMessage> tokenMessages = tokenizeMessage(messageEntities);

            TokenOverflowConfig tokenOverflowConfig = new TokenOverflowConfig();
            tokenOverflowConfig.setStrategyType(strategyType);
            tokenOverflowConfig.setMaxTokens(maxTokens);
            tokenOverflowConfig.setSummaryThreshold(summaryThreshold);
            tokenOverflowConfig.setProviderConfig(providerConfig);
            TokenProcessResult tokenProcessResult = tokenDomainService.processMessages(tokenMessages, tokenOverflowConfig);

            if (tokenProcessResult.isProcessed()) {
                // 保留后的消息列表
                List<TokenMessage> retainedMessages = tokenProcessResult.getRetainedMessages();
                List<String> retainedMessageIds = retainedMessages.stream()
                        .map(TokenMessage::getId)
                        .collect(Collectors.toList());
                
                if (strategyType == TokenOverflowStrategyEnum.SUMMARIZE) {
                    String newSummary = tokenProcessResult.getSummary();
                    String oldSummary = contextEntity.getSummary();
                    contextEntity.setSummary(oldSummary + newSummary);
                }
                contextEntity.setActiveMessages(retainedMessageIds);
            }
        } else {
            contextEntity = new ContextEntity();
            contextEntity.setSessionId(sessionId);
        }

        return new ContextResult(contextEntity, messageEntities);
    }

    /**
     * 将消息实体转换为Token消息
     */
    private List<TokenMessage> tokenizeMessage(List<MessageEntity> messageEntities) {
        return messageEntities.stream().map(message -> {
            TokenMessage tokenMessage = new TokenMessage();
            tokenMessage.setId(message.getId());
            tokenMessage.setRole(message.getRole().name());
            tokenMessage.setContent(message.getContent());
            tokenMessage.setTokenCount(message.getTokenCount());
            tokenMessage.setCreatedAt(message.getCreatedAt());
            return tokenMessage;
        }).collect(Collectors.toList());
    }

    /**
     * 上下文处理结果
     */
    public static class ContextResult {
        private final ContextEntity contextEntity;
        private final List<MessageEntity> messageEntities;

        public ContextResult(ContextEntity contextEntity, List<MessageEntity> messageEntities) {
            this.contextEntity = contextEntity;
            this.messageEntities = messageEntities;
        }

        public ContextEntity getContextEntity() {
            return contextEntity;
        }

        public List<MessageEntity> getMessageEntities() {
            return messageEntities;
        }
    }
} 