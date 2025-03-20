package org.xhy.domain.conversation.service;

import org.xhy.domain.conversation.model.Message;
import org.xhy.domain.conversation.model.MessageDTO;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 发送用户消息
     */
    MessageDTO sendUserMessage(String sessionId, String content);

    /**
     * 保存助手响应消息
     */
    MessageDTO saveAssistantMessage(String sessionId, String content, String provider, String model,
            Integer tokenCount);

    /**
     * 保存系统消息
     */
    MessageDTO saveSystemMessage(String sessionId, String content);

    /**
     * 获取会话的所有消息
     */
    List<MessageDTO> getSessionMessages(String sessionId);

    /**
     * 获取会话的最近N条消息
     */
    List<MessageDTO> getRecentMessages(String sessionId, int count);

    /**
     * 删除消息
     */
    void deleteMessage(String messageId);

    /**
     * 删除会话的所有消息
     */
    void deleteSessionMessages(String sessionId);
}