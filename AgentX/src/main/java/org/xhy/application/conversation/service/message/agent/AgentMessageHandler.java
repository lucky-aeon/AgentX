package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.Agent;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

/** Agent消息处理器 用于支持工具调用的对话模式 实现任务拆分、执行和结果汇总的工作流 使用事件驱动架构进行状态转换 */
@Component(value = "agentMessageHandler")
public class AgentMessageHandler extends AbstractMessageHandler {

    private final AgentToolManager agentToolManager;

    public AgentMessageHandler(LLMServiceFactory llmServiceFactory, AgentToolManager agentToolManager,
            MessageDomainService messageDomainService) {
        super(llmServiceFactory, messageDomainService);
        this.agentToolManager = agentToolManager;
    }

    @Override
    protected ToolProvider provideTools(ChatContext chatContext) {
        return agentToolManager.createToolProvider(agentToolManager.getAvailableTools(chatContext),
                chatContext.getAgent().getToolPresetParams());
    }

    @Override
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userEntity, MessageEntity llmEntity) {

    }
}