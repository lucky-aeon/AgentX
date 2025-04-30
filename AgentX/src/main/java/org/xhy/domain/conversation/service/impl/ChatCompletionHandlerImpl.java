package org.xhy.domain.conversation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ChatCompletionHandler;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.Arrays;
import java.util.List;

/** 聊天完成处理器实现 负责处理聊天完成后的业务逻辑，如保存消息和更新上下文 */
@Service
public class ChatCompletionHandlerImpl implements ChatCompletionHandler {

    private final ConversationDomainService conversationDomainService;
    private final ContextDomainService contextDomainService;

    public ChatCompletionHandlerImpl(ConversationDomainService conversationDomainService,
            ContextDomainService contextDomainService) {
        this.conversationDomainService = conversationDomainService;
        this.contextDomainService = contextDomainService;
    }

    /** 处理聊天完成后的业务逻辑 使用事务确保数据一致性
     *
     * @param userMessage 用户消息实体
     * @param llmMessage LLM回复消息实体
     * @param contextEntity 上下文实体
     * @param inputTokenCount 输入token数量
     * @param outputTokenCount 输出token数量
     * @param llmContent LLM回复内容 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCompletion(MessageEntity userMessage, MessageEntity llmMessage, ContextEntity contextEntity,
            Integer inputTokenCount, Integer outputTokenCount, String llmContent) {
        try {
            // 设置消息的token信息
            userMessage.setTokenCount(inputTokenCount);
            llmMessage.setTokenCount(outputTokenCount);
            llmMessage.setContent(llmContent);

            // 保存消息到数据库
            conversationDomainService.insertBathMessage(Arrays.asList(userMessage, llmMessage));

            // 更新上下文
            if (contextEntity != null) {
                List<String> activeMessages = contextEntity.getActiveMessages();
                activeMessages.add(userMessage.getId());
                activeMessages.add(llmMessage.getId());
                contextDomainService.insertOrUpdate(contextEntity);
            }
        } catch (Exception e) {
            // 记录详细错误信息
            throw new BusinessException("处理聊天完成逻辑失败: " + e.getMessage(), e);
        }
    }
}