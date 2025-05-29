package org.xhy.application.conversation.service.message.preview;

import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.Agent;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 预览消息处理器
 * 专门用于Agent预览功能，不会保存消息到数据库
 */
@Component(value = "previewMessageHandler")
public class PreviewMessageHandler extends AbstractMessageHandler {

    public PreviewMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService) {
        super(llmServiceFactory, messageDomainService);
    }

    /**
     * 预览专用的聊天处理逻辑
     * 与正常流程的区别是不保存消息到数据库
     */
    @Override
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, 
            ChatContext chatContext, MessageEntity userEntity, MessageEntity llmEntity) {
        
        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
        
        // 开始流式对话
        var tokenStream = agent.chat(chatContext.getUserMessage());

        // 错误处理
        tokenStream.onError(throwable -> {
            transport.sendMessage(connection,
                    AgentChatResponse.buildEndMessage(throwable.getMessage(), MessageType.TEXT));
        });

        // 部分响应处理
        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            transport.sendMessage(connection, AgentChatResponse.build(reply, MessageType.TEXT));
        });

        // 完整响应处理 - 预览模式不保存消息
        tokenStream.onCompleteResponse(chatResponse -> {
            // 仅设置token信息，不保存到数据库
            llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
            llmEntity.setContent(chatResponse.aiMessage().text());
            userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());

            // 发送结束消息
            transport.sendEndMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
        });
    }
} 