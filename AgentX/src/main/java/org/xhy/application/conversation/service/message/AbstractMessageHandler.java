package org.xhy.application.conversation.service.message;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.context.AgentPromptTemplates;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.agent.tool.RagToolManager;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;
import org.xhy.application.billing.service.BillingService;
import org.xhy.application.billing.dto.RuleContext;
import org.xhy.infrastructure.exception.InsufficientBalanceException;
import org.xhy.domain.product.constant.BillingType;
import org.xhy.domain.product.constant.UsageDataKeys;
import org.xhy.domain.user.model.AccountEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractMessageHandler {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(AbstractMessageHandler.class);

    /** 连接超时时间（毫秒） */
    protected static final long CONNECTION_TIMEOUT = 3000000L;

    protected final LLMServiceFactory llmServiceFactory;
    protected final MessageDomainService messageDomainService;
    protected final HighAvailabilityDomainService highAvailabilityDomainService;
    protected final SessionDomainService sessionDomainService;
    protected final UserSettingsDomainService userSettingsDomainService;
    protected final LLMDomainService llmDomainService;
    protected final RagToolManager ragToolManager;
    protected final BillingService billingService;
    protected final AccountDomainService accountDomainService;
    public AbstractMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            RagToolManager ragToolManager, BillingService billingService, AccountDomainService accountDomainService) {
        this.llmServiceFactory = llmServiceFactory;
        this.messageDomainService = messageDomainService;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
        this.sessionDomainService = sessionDomainService;
        this.userSettingsDomainService = userSettingsDomainService;
        this.llmDomainService = llmDomainService;
        this.ragToolManager = ragToolManager;
        this.billingService = billingService;
        this.accountDomainService = accountDomainService;
    }

    /** 处理对话的模板方法
     *
     * @param chatContext 对话环境
     * @param transport 消息传输实现
     * @return 连接对象
     * @param <T> 连接类型 */
    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        // 1. 创建连接
        T connection = transport.createConnection(CONNECTION_TIMEOUT);

        // 2. 检查用户余额是否足够
        checkBalanceBeforeChat(chatContext.getUserId(), transport, connection);

        // 3. 创建消息实体
        MessageEntity llmMessageEntity = createLlmMessage(chatContext);
        MessageEntity userMessageEntity = createUserMessage(chatContext);

        // 3. 初始化聊天内存
        MessageWindowChatMemory memory = initMemory();

        // 4. 构建历史消息
        buildHistoryMessage(chatContext, memory);

        // 5. 根据子类决定是否需要工具
        ToolProvider toolProvider = provideTools(chatContext);

        // 6. 根据是否流式选择不同的处理方式
        if (chatContext.isStreaming()) {
            processStreamingChat(chatContext, connection, transport, userMessageEntity, llmMessageEntity, memory,
                    toolProvider);
        } else {
            processSyncChat(chatContext, connection, transport, userMessageEntity, llmMessageEntity, memory,
                    toolProvider);
        }

        return connection;
    }

    /** 子类可以覆盖这个方法提供工具 */
    protected ToolProvider provideTools(ChatContext chatContext) {
        return null; // 默认不提供工具
    }

    /** 流式聊天处理 */
    protected <T> void processStreamingChat(ChatContext chatContext, T connection, MessageTransport<T> transport,
            MessageEntity userEntity, MessageEntity llmEntity, MessageWindowChatMemory memory,
            ToolProvider toolProvider) {

        // 获取流式LLM客户端
        StreamingChatModel streamingClient = llmServiceFactory.getStreamingClient(chatContext.getProvider(),
                chatContext.getModel());

        // 创建流式Agent
        Agent agent = buildStreamingAgent(streamingClient, memory, toolProvider, chatContext.getAgent());

        // 使用现有的流式处理逻辑
        processChat(agent, connection, transport, chatContext, userEntity, llmEntity);
    }

    /** 同步聊天处理 */
    protected <T> void processSyncChat(ChatContext chatContext, T connection, MessageTransport<T> transport,
            MessageEntity userEntity, MessageEntity llmEntity, MessageWindowChatMemory memory,
            ToolProvider toolProvider) {

        // 1. 获取同步LLM客户端
        ChatModel syncClient = llmServiceFactory.getStrandClient(chatContext.getProvider(), chatContext.getModel());

        // 2. 保存用户消息
        messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(userEntity),
                chatContext.getContextEntity());

        try {
            // 3. 记录调用开始时间
            long startTime = System.currentTimeMillis();

            List<ChatMessage> messages = memory.messages();
            messages.add(new UserMessage(chatContext.getUserMessage()));

            // 4. 构建同步Agent并调用
            ChatResponse chatResponse = syncClient.chat(messages);

            // 5. 处理响应 - 设置消息内容
            this.setMessageTokenCount(chatContext.getMessageHistory(), userEntity, llmEntity, chatResponse);

            // 6. 保存消息
            messageDomainService.updateMessage(userEntity);
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                    chatContext.getContextEntity());

            // 7. 发送完整响应
            AgentChatResponse response = new AgentChatResponse(chatResponse.aiMessage().text(), true);
            response.setMessageType(MessageType.TEXT);
            transport.sendEndMessage(connection, response);

            // 8. 上报调用成功结果
            long latency = System.currentTimeMillis() - startTime;
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    true, latency, null);

            // 9. 执行模型调用计费
            performBillingWithErrorHandling(chatContext, chatResponse.tokenUsage().inputTokenCount(),
                    chatResponse.tokenUsage().outputTokenCount(), transport, connection);

        } catch (Exception e) {
            // 错误处理
            AgentChatResponse errorResponse = AgentChatResponse.buildEndMessage(e.getMessage(), MessageType.TEXT);
            transport.sendMessage(connection, errorResponse);

            long latency = System.currentTimeMillis() - System.currentTimeMillis();
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    false, latency, e.getMessage());
        }
    }

    /** 子类实现具体的聊天处理逻辑 */
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userEntity, MessageEntity llmEntity) {

        messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(userEntity),
                chatContext.getContextEntity());

        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());

        // 记录调用开始时间
        long startTime = System.currentTimeMillis();

        tokenStream.onError(throwable -> {
            transport.sendMessage(connection,
                    AgentChatResponse.buildEndMessage(throwable.getMessage(), MessageType.TEXT));

            // 上报调用失败结果
            long latency = System.currentTimeMillis() - startTime;
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    false, latency, throwable.getMessage());
        });

        // 部分响应处理
        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            // 删除换行后消息为空字符串
            if (messageBuilder.get().toString().trim().isEmpty()) {
                return;
            }
            transport.sendMessage(connection, AgentChatResponse.build(reply, MessageType.TEXT));
        });

        // 完整响应处理
        tokenStream.onCompleteResponse(chatResponse -> {
            this.setMessageTokenCount(chatContext.getMessageHistory(), userEntity, llmEntity, chatResponse);

            messageDomainService.updateMessage(userEntity);
            // 保存AI消息
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                    chatContext.getContextEntity());

            // 发送结束消息
            transport.sendEndMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));

            // 上报调用成功结果
            long latency = System.currentTimeMillis() - startTime;
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    true, latency, null);

            // 执行模型调用计费
            performBillingWithErrorHandling(chatContext, chatResponse.tokenUsage().inputTokenCount(),
                    chatResponse.tokenUsage().outputTokenCount(), transport, connection);

            smartRenameSession(chatContext);
        });

        // 错误处理
        // tokenStream.onError(throwable -> handleError(
        // connection, transport, chatContext,
        // messageBuilder.toString(), llmEntity, throwable));

        // 工具执行处理
        tokenStream.onToolExecuted(toolExecution -> {
            if (!messageBuilder.get().isEmpty()) {
                transport.sendMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
                llmEntity.setContent(messageBuilder.get().toString());
                messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                        chatContext.getContextEntity());
                messageBuilder.set(new StringBuilder());
            }
            String message = "执行工具：" + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(toolMessage),
                    chatContext.getContextEntity());

            transport.sendMessage(connection, AgentChatResponse.buildEndMessage(message, MessageType.TOOL_CALL));
        });

        // 启动流处理
        tokenStream.start();
    }

    /** 根据历史消息的本体token算出本次消息的本体token
     * @param historyMessages 历史消息列表
     * @param userEntity 用户请求消息实体
     * @param llmEntity llm回复消息实体
     * @param chatResponse llm响应 */
    private void setMessageTokenCount(List<MessageEntity> historyMessages, MessageEntity userEntity,
            MessageEntity llmEntity, ChatResponse chatResponse) {
        llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
        llmEntity.setBodyTokenCount(chatResponse.tokenUsage().outputTokenCount());
        llmEntity.setContent(chatResponse.aiMessage().text());

        int bodyTokenSum = 0;
        if (CollectionUtil.isNotEmpty(historyMessages)) {
            bodyTokenSum = historyMessages.stream().filter(Objects::nonNull).mapToInt(MessageEntity::getBodyTokenCount)
                    .sum();
        }
        userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
        userEntity.setBodyTokenCount(chatResponse.tokenUsage().inputTokenCount() - bodyTokenSum);
    }

    /** 初始化内存 */
    protected MessageWindowChatMemory initMemory() {
        return MessageWindowChatMemory.builder().maxMessages(1000).chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }

    /** 构建流式Agent */
    protected Agent buildStreamingAgent(StreamingChatModel model, MessageWindowChatMemory memory,
            ToolProvider toolProvider, AgentEntity agent) {

        Map<ToolSpecification, ToolExecutor> ragTools = ragToolManager.createRagTools(agent);

        AiServices<Agent> agentService = AiServices.builder(Agent.class).streamingChatModel(model).chatMemory(memory);

        if (ragTools != null) {
            agentService.tools(ragTools);
        }

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

    // 智能重命名会话
    protected void smartRenameSession(ChatContext chatContext) {
        Thread thread = new Thread(() -> {
            // 获取会话 id
            String sessionId = chatContext.getSessionId();
            // 是否是首次对话
            boolean isFirstConversation = messageDomainService.isFirstConversation(sessionId);
            // 如果首次对话，则重命名会话
            if (isFirstConversation) {
                // 调用用户默认模型进行智能会话名称
                String userId = chatContext.getUserId();
                String userDefaultModelId = userSettingsDomainService.getUserDefaultModelId(userId);
                ModelEntity model = llmDomainService.getModelById(userDefaultModelId);
                // 4. 获取用户降级配置
                List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(userId);

                // 5. 获取服务商信息（支持高可用、会话亲和性和降级）
                HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId,
                        sessionId, fallbackChain);
                ProviderEntity provider = result.getProvider();
                ModelEntity selectedModel = result.getModel();
                ChatModel strandClient = llmServiceFactory.getStrandClient(provider, selectedModel);
                ArrayList<ChatMessage> chatMessages = new ArrayList<>();
                chatMessages.add(new SystemMessage(AgentPromptTemplates.getStartConversationPrompt()));
                chatMessages.add(new UserMessage(chatContext.getUserMessage()));
                ChatResponse chat = strandClient.chat(chatMessages);
                String sessionTitle = chat.aiMessage().text();
                sessionDomainService.updateSession(chatContext.getSessionId(), userId, sessionTitle);

            }
        });
        thread.start();
    }

    /** 创建计费上下文
     *
     * @param chatContext 聊天上下文
     * @param inputTokens 输入Token数量
     * @param outputTokens 输出Token数量
     * @return 计费上下文 */
    private RuleContext createBillingContext(ChatContext chatContext, Integer inputTokens, Integer outputTokens) {
        String requestId = generateRequestId(chatContext.getSessionId(), chatContext.getUserId());

        return RuleContext.builder().type(BillingType.MODEL_USAGE.getCode())
                .serviceId(chatContext.getModel().getId().toString()) // 使用模型表主键ID
                .usageData(Map.of(UsageDataKeys.INPUT_TOKENS, inputTokens != null ? inputTokens : 0,
                        UsageDataKeys.OUTPUT_TOKENS, outputTokens != null ? outputTokens : 0))
                .requestId(requestId).userId(chatContext.getUserId()) // 添加用户ID
                .build();
    }

    /** 生成幂等性请求ID
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 请求ID */
    private String generateRequestId(String sessionId, String userId) {
        long timestamp = System.currentTimeMillis();
        return String.format("billing_%s_%s_%d", sessionId, userId, timestamp);
    }

    /** 执行计费并处理异常
     *
     * @param chatContext 聊天上下文
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @param transport 消息传输
     * @param connection 连接对象 */
    protected <T> void performBillingWithErrorHandling(ChatContext chatContext, Integer inputTokens,
            Integer outputTokens, MessageTransport<T> transport, T connection) {
        try {
            // 创建计费上下文
            RuleContext billingContext = createBillingContext(chatContext, inputTokens, outputTokens);

            // 执行计费
            billingService.charge(billingContext);

            logger.info("模型调用计费成功 - 用户: {}, 模型: {}, 输入Token: {}, 输出Token: {}, 费用已扣除", chatContext.getUserId(),
                    chatContext.getModel().getId(), inputTokens, outputTokens);

        } catch (InsufficientBalanceException e) {
            // 余额不足异常处理
            logger.warn("用户余额不足 - 用户: {}, 模型: {}, 错误: {}", chatContext.getUserId(), chatContext.getModel().getId(),
                    e.getMessage());

            // 发送余额不足提示消息
            AgentChatResponse balanceWarning = new AgentChatResponse("⚠️ 账户余额不足，请及时充值以继续使用服务", false);
            balanceWarning.setMessageType(MessageType.TEXT);
            transport.sendMessage(connection, balanceWarning);

        } catch (BusinessException e) {
            // 业务异常：记录日志但不影响对话
            logger.error("计费业务异常 - 用户: {}, 模型: {}, 错误: {}", chatContext.getUserId(), chatContext.getModel().getId(),
                    e.getMessage(), e);

        } catch (Exception e) {
            // 系统异常：记录日志但不影响对话
            logger.error("计费系统异常 - 用户: {}, 模型: {}, 错误: {}", chatContext.getUserId(), chatContext.getModel().getId(),
                    e.getMessage(), e);
        }
    }

    /** 检查用户余额是否足够开始对话
     *
     * @param userId 用户ID
     * @param transport 消息传输
     * @param connection 连接对象
     * @param <T> 连接类型
     * @throws InsufficientBalanceException 余额不足时抛出 */
    protected <T> void checkBalanceBeforeChat(String userId, MessageTransport<T> transport, T connection) {
        try {
            AccountEntity account = accountDomainService.getOrCreateAccount(userId);
            if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                // 余额不足：发送错误消息
                String errorMessage = "⚠️ 账户余额不足，当前余额：" + account.getBalance() + "元，请充值后继续使用";
                AgentChatResponse errorResponse = AgentChatResponse.buildEndMessage(errorMessage, MessageType.TEXT);
                transport.sendMessage(connection, errorResponse);

                logger.warn("用户余额不足被拒绝对话 - 用户: {}, 当前余额: {}", userId, account.getBalance());
                throw new InsufficientBalanceException("账户余额不足，请充值后继续使用");
            }

            logger.debug("用户余额检查通过 - 用户: {}, 当前余额: {}", userId, account.getBalance());
        } catch (InsufficientBalanceException e) {
            // 重新抛出余额不足异常
            throw e;
        } catch (Exception e) {
            logger.error("余额检查异常 - 用户: {}, 错误: {}", userId, e.getMessage(), e);
            // 余额检查异常时，为了不影响用户体验，允许继续对话
            logger.warn("余额检查服务异常，允许用户继续对话 - 用户: {}", userId);
        }
    }
}
