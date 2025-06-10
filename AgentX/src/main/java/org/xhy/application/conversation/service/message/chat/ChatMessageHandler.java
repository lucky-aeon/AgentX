package org.xhy.application.conversation.service.message.chat;

import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;

/** 标准消息处理器 */
@Component(value = "chatMessageHandler")
public class ChatMessageHandler extends AbstractMessageHandler {

    protected final HighAvailabilityDomainService highAvailabilityDomainService;

    public ChatMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService, HighAvailabilityDomainService highAvailabilityDomainService) {
        super(llmServiceFactory, messageDomainService,highAvailabilityDomainService);
        this.highAvailabilityDomainService = highAvailabilityDomainService;
    }
}