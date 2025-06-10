package org.xhy.application.conversation.service.message.preview;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.Agent;
import org.xhy.application.conversation.service.message.agent.AgentToolManager;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/** 预览消息处理器 专门用于Agent预览功能，不会保存消息到数据库 */
@Component(value = "previewMessageHandler")
public class PreviewMessageHandler extends AbstractMessageHandler {

    private final AgentToolManager agentToolManager;

    protected final HighAvailabilityDomainService highAvailabilityDomainService;

    public PreviewMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            AgentToolManager agentToolManager, HighAvailabilityDomainService highAvailabilityDomainService) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService);
        this.agentToolManager = agentToolManager;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
    }

    @Override
    protected ToolProvider provideTools(ChatContext chatContext) {
        return agentToolManager.createToolProvider(agentToolManager.getAvailableTools(chatContext),
                chatContext.getAgent().getToolPresetParams());
    }

    /** 预览专用的聊天处理逻辑 与正常流程的区别是不保存消息到数据库 */
    @Override
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userEntity, MessageEntity llmEntity) {

        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());

        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());

        tokenStream.onError(throwable -> {
            transport.sendMessage(connection,
                    AgentChatResponse.buildEndMessage(throwable.getMessage(), MessageType.TEXT));
        });

        // 部分响应处理
        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            transport.sendMessage(connection, AgentChatResponse.build(reply, MessageType.TEXT));
        });

        // 完整响应处理
        tokenStream.onCompleteResponse(chatResponse -> {
            // 发送结束消息
            transport.sendEndMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
        });

        // 工具执行处理
        tokenStream.onToolExecuted(toolExecution -> {
            if (!messageBuilder.get().isEmpty()) {
                transport.sendMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
                llmEntity.setContent(messageBuilder.toString());

                messageBuilder.set(new StringBuilder());
            }
            String message = "执行工具：" + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            transport.sendMessage(connection, AgentChatResponse.buildEndMessage(message, MessageType.TOOL_CALL));
        });

        // 启动流处理
        tokenStream.start();
    }
}