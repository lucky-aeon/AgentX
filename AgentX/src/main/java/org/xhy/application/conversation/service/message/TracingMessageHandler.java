package org.xhy.application.conversation.service.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import org.xhy.application.billing.service.BillingService;
import org.xhy.application.conversation.service.handler.Agent;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.handler.context.TracingChatContext;
import org.xhy.application.conversation.service.message.agent.tool.RagToolManager;
import org.xhy.application.trace.collector.TraceCollector;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.trace.constant.ExecutionPhase;
import org.xhy.domain.trace.model.ModelCallInfo;
import org.xhy.domain.trace.model.ToolCallInfo;
import org.xhy.domain.trace.model.TraceContext;
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 带追踪功能的消息处理器基类 在关键节点集成链路追踪逻辑 */
public abstract class TracingMessageHandler extends AbstractMessageHandler {

    protected final TraceCollector traceCollector;

    public TracingMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            RagToolManager ragToolManager, BillingService billingService, AccountDomainService accountDomainService,
            TraceCollector traceCollector) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService, sessionDomainService,
                userSettingsDomainService, llmDomainService, ragToolManager, billingService, accountDomainService);
        this.traceCollector = traceCollector;
    }
}