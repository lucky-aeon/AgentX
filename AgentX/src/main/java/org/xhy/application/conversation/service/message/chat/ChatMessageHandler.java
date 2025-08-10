package org.xhy.application.conversation.service.message.chat;

import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.TracingMessageHandler;
import org.xhy.application.conversation.service.message.agent.tool.RagToolManager;
import org.xhy.application.trace.collector.TraceCollector;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.application.billing.service.BillingService;
import org.xhy.domain.user.service.AccountDomainService;

/** 标准消息处理器 */
@Component(value = "chatMessageHandler")
public class ChatMessageHandler extends TracingMessageHandler {

    public ChatMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            RagToolManager ragToolManager, BillingService billingService, AccountDomainService accountDomainService,
            TraceCollector traceCollector) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService, sessionDomainService,
                userSettingsDomainService, llmDomainService, ragToolManager, billingService, accountDomainService,
                traceCollector);
    }
}