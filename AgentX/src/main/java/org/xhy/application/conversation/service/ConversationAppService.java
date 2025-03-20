package org.xhy.application.conversation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.domain.conversation.model.MessageDTO;
import org.xhy.domain.conversation.service.ConversationService;

import java.util.function.BiConsumer;

/**
 * 对话应用服务，用于适配域层的对话服务
 */
@Service
public class ConversationAppService {

    private final ConversationService conversationService;
    private final org.xhy.application.conversation.service.ConversationService applicationConversationService;

    public ConversationAppService(
            ConversationService conversationService,
            org.xhy.application.conversation.service.ConversationService applicationConversationService) {
        this.conversationService = conversationService;
        this.applicationConversationService = applicationConversationService;
    }

    /**
     * 发送消息并获取流式回复
     */
    public SseEmitter chat(String sessionId, String content) {
        return conversationService.chat(sessionId, content);
    }

    /**
     * 发送消息并获取同步回复（非流式）
     */
    public MessageDTO chatSync(String sessionId, String content) {
        return conversationService.chatSync(sessionId, content);
    }

    /**
     * 创建新会话并发送第一条消息
     */
    public SseEmitter createSessionAndChat(String title, String userId, String content) {
        return conversationService.createSessionAndChat(title, userId, content);
    }

    /**
     * 清除会话上下文
     */
    public void clearContext(String sessionId) {
        conversationService.clearContext(sessionId);
    }

    /**
     * 处理聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        return applicationConversationService.chat(request);
    }

    /**
     * 处理流式聊天请求
     */
    public void chatStream(StreamChatRequest request, BiConsumer<StreamChatResponse, Boolean> responseHandler) {
        applicationConversationService.chatStream(request, responseHandler);
    }
}