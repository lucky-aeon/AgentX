package org.xhy.application.conversation.service.message.chat;

import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.Agent;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

/** 标准消息处理器 */
@Component(value = "chatMessageHandler")
public class ChatMessageHandler extends AbstractMessageHandler {

    public ChatMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService) {
        super(llmServiceFactory, messageDomainService);
    }

    @Override
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userEntity, MessageEntity llmEntity) {

    }
}