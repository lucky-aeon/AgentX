package org.xhy.application.conversation.service.message;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.context.AgentPromptTemplates;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractMessageHandler {

    protected static final long CONNECTION_TIMEOUT = 3000000L;

    protected final LLMServiceFactory llmServiceFactory;
    protected final MessageDomainService messageDomainService;

    public AbstractMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService) {
        this.llmServiceFactory = llmServiceFactory;
        this.messageDomainService = messageDomainService;
    }

    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        T connection = transport.createConnection(CONNECTION_TIMEOUT);

        StreamingChatLanguageModel model = llmServiceFactory.getStreamingClient(chatContext.getProvider(),
                chatContext.getModel());

        MessageEntity llmMessageEntity = createLlmMessage(chatContext);
        MessageEntity userMessageEntity = createUserMessage(chatContext);

        messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(userMessageEntity),
                chatContext.getContextEntity());

        MessageWindowChatMemory memory = initMemory();
        buildHistoryMessage(chatContext, memory);

        ToolProvider toolProvider = provideTools(chatContext);
        Agent agent = buildAgent(model, memory, toolProvider);

        processChat(agent, connection, transport, chatContext, userMessageEntity, llmMessageEntity);

        return connection;
    }

    protected ToolProvider provideTools(ChatContext chatContext) {
        return null;
    }

    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userEntity, MessageEntity llmEntity) {
        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());

        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            transport.sendMessage(connection, AgentChatResponse.build(reply, MessageType.TEXT));
        });

        tokenStream.onCompleteResponse(chatResponse -> {
            llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
            llmEntity.setContent(chatResponse.aiMessage().text());

            userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
            messageDomainService.updateMessage(userEntity);

            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                    chatContext.getContextEntity());

            transport.sendEndMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
        });

        tokenStream.onError(throwable -> handleError(connection, transport, chatContext, messageBuilder.toString(),
                llmEntity, throwable));

        tokenStream.onToolExecuted(toolExecution -> {
            if (!messageBuilder.get().isEmpty()) {
                transport.sendMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
                llmEntity.setContent(messageBuilder.toString());
                messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                        chatContext.getContextEntity());
                messageBuilder.set(new StringBuilder());
            }
            String message = "执行工具: " + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(toolMessage),
                    chatContext.getContextEntity());

            transport.sendMessage(connection, AgentChatResponse.buildEndMessage(message, MessageType.TOOL_CALL));
        });

        tokenStream.start();
    }

    protected MessageWindowChatMemory initMemory() {
        return MessageWindowChatMemory.builder().maxMessages(1000).chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }

    protected Agent buildAgent(StreamingChatLanguageModel model, MessageWindowChatMemory memory,
            ToolProvider toolProvider) {
        AiServices<Agent> agentService = AiServices.builder(Agent.class).streamingChatLanguageModel(model)
                .chatMemory(memory);

        if (toolProvider != null) {
            agentService.toolProvider(toolProvider);
        }

        return agentService.build();
    }

    protected MessageEntity createUserMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.USER);
        messageEntity.setContent(environment.getUserMessage());
        messageEntity.setSessionId(environment.getSessionId());
        return messageEntity;
    }

    protected MessageEntity createLlmMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.ASSISTANT);
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setModel(environment.getModel().getModelId());
        messageEntity.setProvider(environment.getProvider().getId());
        return messageEntity;
    }

    protected void buildHistoryMessage(ChatContext chatContext, MessageWindowChatMemory memory) {
        String summary = chatContext.getContextEntity().getSummary();
        if (StringUtils.isNotEmpty(summary)) {
            memory.add(new AiMessage(AgentPromptTemplates.getSummaryPrefix() + summary));
        }
        memory.add(new SystemMessage(
                chatContext.getAgent().getSystemPrompt() + "\n" + AgentPromptTemplates.getIgnoreSensitiveInfoPrompt()));
        List<MessageEntity> messageHistory = chatContext.getMessageHistory();
        for (MessageEntity messageEntity : messageHistory) {
            if (messageEntity.isUserMessage()) {
                memory.add(new UserMessage(messageEntity.getContent()));
            } else if (messageEntity.isAIMessage()) {
                memory.add(new AiMessage(messageEntity.getContent()));
            } else if (messageEntity.isSystemMessage()) {
                memory.add(new SystemMessage(messageEntity.getContent()));
            }
        }
    }

    protected <T> void handleError(T connection, MessageTransport<T> transport, ChatContext chatContext, String message,
            MessageEntity llmEntity, Throwable throwable) {
        OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4o");
        int usedToken = tokenizer.estimateTokenCountInMessage(new AiMessage(message));
        llmEntity.setTokenCount(usedToken);
        llmEntity.setContent(message);

        messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                chatContext.getContextEntity());

        transport.sendEndMessage(connection,
                AgentChatResponse.buildEndMessage(throwable.getMessage(), MessageType.TEXT));
    }
}
