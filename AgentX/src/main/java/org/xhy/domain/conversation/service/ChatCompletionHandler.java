package org.xhy.domain.conversation.service;

import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;

/**
 * 聊天完成处理器接口
 * 负责处理聊天完成后的业务逻辑，如保存消息和更新上下文
 */
public interface ChatCompletionHandler {

    /**
     * 处理聊天完成后的业务逻辑
     *
     * @param userMessage     用户消息实体
     * @param llmMessage      LLM回复消息实体
     * @param contextEntity   上下文实体
     * @param inputTokenCount 输入token数量
     * @param outputTokenCount 输出token数量
     * @param llmContent      LLM回复内容
     */
    void handleCompletion(
            MessageEntity userMessage,
            MessageEntity llmMessage,
            ContextEntity contextEntity,
            Integer inputTokenCount,
            Integer outputTokenCount,
            String llmContent);
} 