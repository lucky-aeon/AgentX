package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.TracingMessageHandler;
import org.xhy.application.conversation.service.message.builtin.BuiltInToolRegistry;
import org.xhy.application.conversation.service.ChatSessionManager;
import org.xhy.application.trace.collector.TraceCollector;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.application.billing.service.BillingService;
import org.xhy.domain.user.service.AccountDomainService;

/** Agent消息处理器 用于支持工具调用的对话模式 实现任务拆分、执行和结果汇总的工作流 使用事件驱动架构进行状态转换 */
@Component(value = "agentMessageHandler")
public class AgentMessageHandler extends TracingMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentMessageHandler.class);
    private AgentToolManager agentToolManager;

    public AgentMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            BuiltInToolRegistry builtInToolRegistry, BillingService billingService,
            AccountDomainService accountDomainService, ChatSessionManager chatSessionManager,
            TraceCollector traceCollector, AgentToolManager agentToolManager) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService, sessionDomainService,
                userSettingsDomainService, llmDomainService, builtInToolRegistry, billingService, accountDomainService,
                chatSessionManager, traceCollector);
        this.agentToolManager = agentToolManager;
    }

    @Override
    protected ToolProvider provideTools(ChatContext chatContext) {
        logger.debug("🔧 [提供工具] Agent: {}, MCP服务器: {}",
            chatContext.getAgent().getName(), chatContext.getMcpServerNames());
        // 统一通过AgentToolManager创建：合并 MCP + 子Agent 工具
        ToolProvider toolProvider = agentToolManager.createToolProvider(chatContext);
        logger.debug("🔧 [工具提供者] 已创建: {}", toolProvider != null ? "是" : "否");
        return toolProvider;
    }
}
