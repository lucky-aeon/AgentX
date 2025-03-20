package org.xhy.application.conversation.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.conversation.model.MessageDTO;
import org.xhy.domain.conversation.service.MessageService;

import java.util.List;

/**
 * 消息应用服务，用于适配域层的消息服务
 */
@Service
public class MessageAppService {

    private final MessageService messageService;

    public MessageAppService(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 发送用户消息
     */
    public MessageDTO sendUserMessage(String sessionId, String content) {
        return messageService.sendUserMessage(sessionId, content);
    }

    /**
     * 保存助手消息
     */
    public MessageDTO saveAssistantMessage(String sessionId, String content, String provider, String model,
            Integer tokenCount) {
        return messageService.saveAssistantMessage(sessionId, content, provider, model, tokenCount);
    }

    /**
     * 保存系统消息
     */
    public MessageDTO saveSystemMessage(String sessionId, String content) {
        return messageService.saveSystemMessage(sessionId, content);
    }

    /**
     * 获取会话消息列表
     */
    public List<MessageDTO> getSessionMessages(String sessionId) {
        return messageService.getSessionMessages(sessionId);
    }

    /**
     * 获取会话最近消息
     */
    public List<MessageDTO> getRecentMessages(String sessionId, int count) {
        return messageService.getRecentMessages(sessionId, count);
    }

    /**
     * 删除消息
     */
    public void deleteMessage(String messageId) {
        messageService.deleteMessage(messageId);
    }

    /**
     * 删除会话所有消息
     */
    public void deleteSessionMessages(String sessionId) {
        messageService.deleteSessionMessages(sessionId);
    }
}