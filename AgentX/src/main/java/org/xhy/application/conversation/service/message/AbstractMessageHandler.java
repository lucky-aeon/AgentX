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

    /**
     * 连接超时时间（毫秒）
     */
    protected static final long CONNECTION_TIMEOUT = 3000000L;

    protected final LLMServiceFactory llmServiceFactory;
    protected final MessageDomainService messageDomainService;


    public AbstractMessageHandler(
            LLMServiceFactory llmServiceFactory,
            MessageDomainService messageDomainService) {
        this.llmServiceFactory = llmServiceFactory;
        this.messageDomainService = messageDomainService;
    }

    /**
     * 处理对话的模板方法
     *
     * @param chatContext 对话环境
     * @param transport 消息传输实现
     * @return 连接对象
     * @param <T> 连接类型
     */
    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        // 1. 创建连接
        T connection = transport.createConnection(CONNECTION_TIMEOUT);

        // 2. 获取LLM客户端
        StreamingChatLanguageModel model = llmServiceFactory.getStreamingClient(
                chatContext.getProvider(), chatContext.getModel());

        // 3. 创建消息实体
        MessageEntity llmMessageEntity = createLlmMessage(chatContext);
        MessageEntity userMessageEntity = createUserMessage(chatContext);

        // 4. 保存用户消息和更新上下文
        messageDomainService.saveMessageAndUpdateContext(
                Collections.singletonList(userMessageEntity), 
                chatContext.getContextEntity());

        // 5. 初始化聊天内存
        MessageWindowChatMemory memory = initMemory();
        
        // 6. 构建历史消息
        buildHistoryMessage(chatContext, memory);
        
        // 7. 根据子类决定是否需要工具
        ToolProvider toolProvider = provideTools();
        
        // 8. 创建Agent
        Agent agent = buildAgent(model, memory, toolProvider);

        // 9. 处理聊天
        processChat(agent, connection, transport, chatContext, 
                userMessageEntity, llmMessageEntity);
        
        return connection;
    }

    /**
     * 子类可以覆盖这个方法提供工具
     */
    protected ToolProvider provideTools() {
        return null; // 默认不提供工具
    }

    /**
     * 子类实现具体的聊天处理逻辑
     */
    protected <T> void processChat(
        Agent agent, T connection, MessageTransport<T> transport,
        ChatContext chatContext, MessageEntity userEntity, MessageEntity llmEntity){
        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());
        tokenStream.ignoreErrors();
        // 部分响应处理
        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            transport.sendMessage(connection,
                    AgentChatResponse.build(reply, MessageType.TEXT));
        });

        // 完整响应处理
        tokenStream.onCompleteResponse(chatResponse -> {
            // 更新token信息
            llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
            llmEntity.setContent(chatResponse.aiMessage().text());

            userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
            messageDomainService.updateMessage(userEntity);

            // 保存AI消息
            messageDomainService.saveMessageAndUpdateContext(
                    Collections.singletonList(llmEntity),
                    chatContext.getContextEntity());

            // 发送结束消息
            transport.sendEndMessage(connection,
                    AgentChatResponse.buildEndMessage(MessageType.TEXT));
        });

        // 错误处理
//        tokenStream.onError(throwable -> handleError(
//                connection, transport, chatContext,
//                messageBuilder.toString(), llmEntity, throwable));

        // 工具执行处理
        tokenStream.onToolExecuted(toolExecution -> {
            if (!messageBuilder.get().isEmpty()){
                transport.sendMessage(connection,
                        AgentChatResponse.buildEndMessage(MessageType.TEXT));
                llmEntity.setContent(messageBuilder.toString());
                messageDomainService.saveMessageAndUpdateContext(
                        Collections.singletonList(llmEntity),
                        chatContext.getContextEntity());
                messageBuilder.set(new StringBuilder());
            }
            String message = "执行工具：" + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            messageDomainService.saveMessageAndUpdateContext(
                    Collections.singletonList(toolMessage),
                    chatContext.getContextEntity());

            transport.sendMessage(connection,
                    AgentChatResponse.buildEndMessage(message, MessageType.TOOL_CALL));
        });

        // 启动流处理
        tokenStream.start();
    }

    /**
     * 初始化内存
     */
    protected MessageWindowChatMemory initMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(1000)
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }

    /**
     * 构建Agent
     */
    protected Agent buildAgent(
            StreamingChatLanguageModel model, 
            MessageWindowChatMemory memory,
            ToolProvider toolProvider) {
        AiServices<Agent> agentService = AiServices.builder(Agent.class)
                .streamingChatLanguageModel(model)
                .chatMemory(memory);

        if (toolProvider != null) {
            agentService.toolProvider(toolProvider);
        }

        return agentService.build();
    }

    /**
     * 创建用户消息实体
     */
    protected MessageEntity createUserMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.USER);
        messageEntity.setContent(environment.getUserMessage());
        messageEntity.setSessionId(environment.getSessionId());
        return messageEntity;
    }

    /**
     * 创建LLM消息实体
     */
    protected MessageEntity createLlmMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.ASSISTANT);
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setModel(environment.getModel().getModelId());
        messageEntity.setProvider(environment.getProvider().getId());
        return messageEntity;
    }


    /**
     * 构建历史消息到内存中
     */
    protected void buildHistoryMessage(ChatContext chatContext, MessageWindowChatMemory memory) {
        String summary = chatContext.getContextEntity().getSummary();
        if (StringUtils.isNotEmpty(summary)) {
            // 添加为AI消息，但明确标识这是摘要
            memory.add(new AiMessage(AgentPromptTemplates.getSummaryPrefix() + summary));
        }
        memory.add(new SystemMessage(chatContext.getAgent().getSystemPrompt()+"\n"+AgentPromptTemplates.getIgnoreSensitiveInfoPrompt()));
        List<MessageEntity> messageHistory = chatContext.getMessageHistory();
        for (MessageEntity messageEntity : messageHistory) {
            if(messageEntity.isUserMessage()){
                memory.add(new UserMessage(messageEntity.getContent()));
            }else if(messageEntity.isAIMessage()){
                memory.add(new AiMessage(messageEntity.getContent()));
            }else if(messageEntity.isSystemMessage()){
                memory.add(new SystemMessage(messageEntity.getContent()));
            }
        }
    }
    
    /**
     * 错误处理辅助方法
     */
    protected <T> void handleError(
            T connection, MessageTransport<T> transport,
            ChatContext chatContext, String message,
            MessageEntity llmEntity, Throwable throwable) {
            
        // 记录token
        OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4o");
        int usedToken = tokenizer.estimateTokenCountInMessage(
                new AiMessage(message));
        llmEntity.setTokenCount(usedToken);
        llmEntity.setContent(message);
        
        messageDomainService.saveMessageAndUpdateContext(
                Collections.singletonList(llmEntity),
                chatContext.getContextEntity());
                
        transport.sendEndMessage(connection, 
                AgentChatResponse.buildEndMessage(
                        throwable.getMessage(), MessageType.TEXT));
    }
}
