package org.xhy.application.conversation.service.message;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.context.AgentPromptTemplates;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;
import org.xhy.infrastructure.exception.StreamInterruptedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(AbstractMessageHandler.class);

    /** 连接超时时间（毫秒） */
    protected static final long CONNECTION_TIMEOUT = 3000000L;

    protected final LLMServiceFactory llmServiceFactory;
    protected final MessageDomainService messageDomainService;

    public AbstractMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService) {
        this.llmServiceFactory = llmServiceFactory;
        this.messageDomainService = messageDomainService;
    }

    /*
     * @param chatContext 对话环境
     * 
     * @param transport 消息传输实现
     * 
     * @return 连接对象
     * 
     * @param <T> 连接类型
     */
    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        String sessionId = chatContext.getSessionId();

        // 处理已存在的流
        StreamStateManager.handleExistingStream(sessionId, transport);

        // 2. 创建新的连接和状态
        T connection = transport.createConnection(CONNECTION_TIMEOUT);
        StreamStateManager.StreamState newState = StreamStateManager.createState(sessionId, connection);

        try {
            // 3. 获取LLM客户端
            StreamingChatModel streamingClient = llmServiceFactory.getStreamingClient(chatContext.getProvider(),
                    chatContext.getModel());

            // 4. 创建消息实体
            MessageEntity llmMessageEntity = createLlmMessage(chatContext);
            MessageEntity userMessageEntity = createUserMessage(chatContext);

            // 5. 保存用户消息和更新上下文
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(userMessageEntity),
                    chatContext.getContextEntity());

            // 6. 初始化聊天内存
            MessageWindowChatMemory memory = initMemory();

            // 7. 构建历史消息
            buildHistoryMessage(chatContext, memory);

            // 8. 根据子类决定是否需要工具
            ToolProvider toolProvider = provideTools(chatContext);

            // 9. 创建Agent
            Agent agent = buildAgent(streamingClient, memory, toolProvider);

            // 10. 处理聊天
            processChat(agent, transport, chatContext, userMessageEntity, llmMessageEntity);

            return connection;
        } catch (Exception e) {
            StreamStateManager.StreamState state = StreamStateManager.getState(sessionId);
            if (state != null) {
                state.setActive(false);
                state.setCompleted(true);
            }
            transport.handleError(connection, e);
            transport.completeConnection(connection);
            StreamStateManager.removeState(sessionId);
            throw e;
        }
    }

    /** 子类可以覆盖这个方法提供工具 */
    protected ToolProvider provideTools(ChatContext chatContext) {
        return null; // 默认不提供工具
    }

    /** 子类实现具体的聊天处理逻辑 */
    protected <T> void processChat(Agent agent, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userMessageEntity, MessageEntity llmEntity) {
        String sessionId = chatContext.getSessionId();
        StreamStateManager.StreamState state = StreamStateManager.getState(sessionId);
        if (state == null)
            return;

        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());

        tokenStream.onPartialResponse(reply -> {
            if (state == null || !state.isActive() || state.isCompleted()) {
                throw new StreamInterruptedException(state != null ? state.getPartialContent().toString() : "");
            }

            try {
                state.getPartialContent().append(reply);
                transport.sendMessage((T) state.getConnection(), AgentChatResponse.build(reply, MessageType.TEXT));
            } catch (Exception e) {
                state.setActive(false);
                throw new StreamInterruptedException(state.getPartialContent().toString());
            }
        });

        tokenStream.onCompleteResponse(chatResponse -> {
            if (state == null || state.isCompleted())
                return;

            try {
                if (state.isActive()) {
                    llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
                    llmEntity.setContent(state.getPartialContent().toString());
                    userMessageEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
                    messageDomainService.updateMessage(userMessageEntity);
                    messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                            chatContext.getContextEntity());
                    transport.sendEndMessage((T) state.getConnection(),
                            AgentChatResponse.buildEndMessage(MessageType.TEXT));
                }
            } finally {
                state.setCompleted(true);
                transport.completeConnection((T) state.getConnection());
                StreamStateManager.removeState(sessionId);
            }
        });

        tokenStream.onError(throwable -> {
            if (state == null || state.isCompleted())
                return;

            try {
                if (throwable instanceof StreamInterruptedException) {
                    log.info("会话 [{}] 被 StreamInterruptedException 中断", sessionId);
                    if (state.getPartialContent().length() > 0) {
                        llmEntity.setContent(state.getPartialContent().toString());
                        llmEntity.setRole(Role.ASSISTANT);
                        messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                                chatContext.getContextEntity());
                        log.debug("已经保存内容：{}", state.getPartialContent());
                        state.getPartialContent().setLength(0);
                        log.debug("已清空保存内容，目前的内容为：{}", state.getPartialContent());
                    }
                } else {
                    transport.handleError((T) state.getConnection(), throwable);
                }
            } finally {
                state.setActive(true);
                state.setCompleted(true);
                transport.completeConnection((T) state.getConnection());
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            if (state == null || !state.isActive() || state.isCompleted()) {
                throw new StreamInterruptedException(state != null ? state.getPartialContent().toString() : "");
            }

            if (state.getPartialContent().length() > 0) {
                transport.sendMessage((T) state.getConnection(), AgentChatResponse.buildEndMessage(MessageType.TEXT));
                MessageEntity preToolAiMessage = createLlmMessage(chatContext);
                preToolAiMessage.setContent(state.getPartialContent().toString());
                messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(preToolAiMessage),
                        chatContext.getContextEntity());
                state.getPartialContent().setLength(0);
            }

            String message = "执行工具：" + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(toolMessage),
                    chatContext.getContextEntity());
            transport.sendMessage((T) state.getConnection(),
                    AgentChatResponse.buildEndMessage(message, MessageType.TOOL_CALL));
        });

        tokenStream.start();
    }

    /** 初始化内存 */
    protected MessageWindowChatMemory initMemory() {
        return MessageWindowChatMemory.builder().maxMessages(1000).chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }

    /** 构建Agent */
    protected Agent buildAgent(StreamingChatModel model, MessageWindowChatMemory memory, ToolProvider toolProvider) {
        AiServices<Agent> agentService = AiServices.builder(Agent.class).streamingChatModel(model).chatMemory(memory);

        if (toolProvider != null) {
            agentService.toolProvider(toolProvider);
        }

        return agentService.build();
    }

    /** 创建用户消息实体 */
    protected MessageEntity createUserMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.USER);
        messageEntity.setContent(environment.getUserMessage());
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setFileUrls(environment.getFileUrls());
        return messageEntity;
    }

    /** 创建LLM消息实体 */
    protected MessageEntity createLlmMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.ASSISTANT);
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setModel(environment.getModel().getModelId());
        messageEntity.setProvider(environment.getProvider().getId());
        return messageEntity;
    }

    /** 构建历史消息到内存中 */
    protected void buildHistoryMessage(ChatContext chatContext, MessageWindowChatMemory memory) {
        String summary = chatContext.getContextEntity().getSummary();
        if (StringUtils.isNotEmpty(summary)) {
            // 添加为AI消息，但明确标识这是摘要
            memory.add(new AiMessage(AgentPromptTemplates.getSummaryPrefix() + summary));
        }

        String presetToolPrompt = "";
        // 设置预先工具设置的参数到系统提示词中
        Map<String, Map<String, Map<String, String>>> toolPresetParams = chatContext.getAgent().getToolPresetParams();
        if (toolPresetParams != null) {
            presetToolPrompt = AgentPromptTemplates.generatePresetToolPrompt(toolPresetParams);
        }

        memory.add(new SystemMessage(chatContext.getAgent().getSystemPrompt() + "\n" + presetToolPrompt));
        List<MessageEntity> messageHistory = chatContext.getMessageHistory();
        for (MessageEntity messageEntity : messageHistory) {
            if (messageEntity.isUserMessage()) {
                List<String> fileUrls = messageEntity.getFileUrls();
                for (String fileUrl : fileUrls) {
                    memory.add(UserMessage.from(ImageContent.from(fileUrl)));
                }
                if (!StringUtils.isEmpty(messageEntity.getContent())) {
                    memory.add(new UserMessage(messageEntity.getContent()));
                }
            } else if (messageEntity.isAIMessage()) {
                memory.add(new AiMessage(messageEntity.getContent()));
            } else if (messageEntity.isSystemMessage()) {
                memory.add(new SystemMessage(messageEntity.getContent()));
            }
        }
    }

    protected abstract <T> void processChat(Agent agent, T connection, MessageTransport<T> transport,
            ChatContext chatContext, MessageEntity userEntity, MessageEntity llmEntity);

}
