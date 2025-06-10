package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;

/** Agent消息处理器 用于支持工具调用的对话模式 实现任务拆分、执行和结果汇总的工作流 使用事件驱动架构进行状态转换 */
@Component(value = "agentMessageHandler")
public class AgentMessageHandler extends AbstractMessageHandler {

    private final AgentToolManager agentToolManager;

    protected final HighAvailabilityDomainService highAvailabilityDomainService;

    public AgentMessageHandler(LLMServiceFactory llmServiceFactory, AgentToolManager agentToolManager,
            MessageDomainService messageDomainService, HighAvailabilityDomainService highAvailabilityDomainService) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService);
        this.agentToolManager = agentToolManager;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
    }

    @Override
    protected ToolProvider provideTools(ChatContext chatContext) {
        return agentToolManager.createToolProvider(agentToolManager.getAvailableTools(chatContext),
                chatContext.getAgent().getToolPresetParams());
    }
}