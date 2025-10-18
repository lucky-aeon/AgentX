package org.xhy.application.conversation.service;

import cn.hutool.core.bean.BeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.AgentPreviewRequest;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.application.conversation.dto.RagChatRequest;
import org.xhy.application.conversation.service.message.rag.RagChatContext;
import org.xhy.application.rag.dto.RagSearchRequest;
import org.xhy.application.rag.dto.RagStreamChatRequest;
import org.xhy.interfaces.dto.agent.request.WidgetChatRequest;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.preview.PreviewMessageHandler;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.user.service.UserSettingsDomainService;

import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.model.AgentWidgetEntity;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.agent.service.AgentWorkspaceDomainService;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.handler.MessageHandlerFactory;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.token.service.TokenDomainService;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.UserToolDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.transport.MessageTransport;
import org.xhy.infrastructure.transport.MessageTransportFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 对话应用服务，用于适配域层的对话服务 */
@Service
public class ConversationAppService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationAppService.class);

    private final ConversationDomainService conversationDomainService;
    private final SessionDomainService sessionDomainService;
    private final AgentDomainService agentDomainService;
    private final AgentWorkspaceDomainService agentWorkspaceDomainService;
    private final LLMDomainService llmDomainService;
    private final ContextDomainService contextDomainService;
    private final TokenDomainService tokenDomainService;
    private final MessageDomainService messageDomainService;

    private final MessageHandlerFactory messageHandlerFactory;
    private final MessageTransportFactory transportFactory;

    private final UserToolDomainService userToolDomainService;
    private final UserSettingsDomainService userSettingsDomainService;
    private final PreviewMessageHandler previewMessageHandler;
    private final HighAvailabilityDomainService highAvailabilityDomainService;
    private final RagSessionManager ragSessionManager;
    private final ChatSessionManager chatSessionManager;

    public ConversationAppService(ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService, AgentDomainService agentDomainService,
            AgentWorkspaceDomainService agentWorkspaceDomainService, LLMDomainService llmDomainService,
            ContextDomainService contextDomainService, TokenDomainService tokenDomainService,
            MessageDomainService messageDomainService, MessageHandlerFactory messageHandlerFactory,
            MessageTransportFactory transportFactory, UserToolDomainService toolDomainService,
            UserSettingsDomainService userSettingsDomainService, PreviewMessageHandler previewMessageHandler,
            HighAvailabilityDomainService highAvailabilityDomainService, RagSessionManager ragSessionManager,
            ChatSessionManager chatSessionManager) {
        this.conversationDomainService = conversationDomainService;
        this.sessionDomainService = sessionDomainService;
        this.agentDomainService = agentDomainService;
        this.agentWorkspaceDomainService = agentWorkspaceDomainService;
        this.llmDomainService = llmDomainService;
        this.contextDomainService = contextDomainService;
        this.tokenDomainService = tokenDomainService;
        this.messageDomainService = messageDomainService;
        this.messageHandlerFactory = messageHandlerFactory;
        this.transportFactory = transportFactory;
        this.userToolDomainService = toolDomainService;
        this.userSettingsDomainService = userSettingsDomainService;
        this.previewMessageHandler = previewMessageHandler;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
        this.ragSessionManager = ragSessionManager;
        this.chatSessionManager = chatSessionManager;
    }

    /** 获取会话中的消息列表
     *
     * @param sessionId 会话id
     * @param userId 用户id
     * @return 消息列表 */
    public List<MessageDTO> getConversationMessages(String sessionId, String userId) {
        // 查询对应会话是否存在
        SessionEntity sessionEntity = sessionDomainService.find(sessionId, userId);

        if (sessionEntity == null) {
            throw new BusinessException("会话不存在");
        }

        List<MessageEntity> conversationMessages = conversationDomainService.getConversationMessages(sessionId);
        return MessageAssembler.toDTOs(conversationMessages);
    }

    /** 对话方法 - 统一入口，支持根据请求类型自动选择处理器
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return SSE发射器 */
    public SseEmitter chat(ChatRequest chatRequest, String userId) {
        // 1. 根据请求类型准备对话环境
        ChatContext environment = prepareEnvironmentByRequestType(chatRequest, userId);

        // 2. 获取传输方式 (当前仅支持SSE，将来支持WebSocket)
        MessageTransport<SseEmitter> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 3. 根据请求类型获取适合的消息处理器
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(chatRequest);

        // 4. 处理对话
        SseEmitter emitter = handler.chat(environment, transport);

        // 5. 注册会话到会话管理器（支持中断功能）
        chatSessionManager.registerSession(chatRequest.getSessionId(), emitter);

        return emitter;
    }

    /** 对话处理（支持指定模型）- 用于外部API
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @param modelId 指定的模型ID（可选，为null时使用Agent绑定的模型）
     * @return SSE发射器 */
    public SseEmitter chatWithModel(ChatRequest chatRequest, String userId, String modelId) {
        // 1. 准备对话环境（支持指定模型）
        ChatContext environment = prepareEnvironmentWithModel(chatRequest, userId, modelId);

        // 2. 获取传输方式 (当前仅支持SSE，将来支持WebSocket)
        MessageTransport<SseEmitter> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 3. 获取适合的消息处理器 (根据agent类型)
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent());

        // 4. 处理对话
        SseEmitter emitter = handler.chat(environment, transport);

        // 5. 注册会话到会话管理器（支持中断功能）
        chatSessionManager.registerSession(chatRequest.getSessionId(), emitter);

        return emitter;
    }

    /** 同步对话处理（支持指定模型）- 用于外部API
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @param modelId 指定的模型ID（可选，为null时使用Agent绑定的模型）
     * @return 同步聊天响应 */
    public ChatResponse chatSyncWithModel(ChatRequest chatRequest, String userId, String modelId) {
        // 1. 准备对话环境（设置为非流式）
        ChatContext environment = prepareEnvironmentWithModel(chatRequest, userId, modelId);
        environment.setStreaming(false); // 设置为同步模式

        // 2. 获取同步传输方式
        MessageTransport<ChatResponse> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SYNC);

        // 3. 获取适合的消息处理器
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent());

        // 4. 处理对话
        return handler.chat(environment, transport);
    }

    /** 同步对话（指定子Agent，仍使用同一session）
     * 用于 Multi-Agent 子Agent 调用：在相同的 sessionId 下，按指定 agentId 构造上下文并执行同步对话。
     */
    public ChatResponse chatSyncWithAgent(ChatRequest chatRequest, String userId, String targetAgentId) {
        return chatSyncWithAgent(chatRequest, userId, targetAgentId, false);
    }

    /** 同步对话（可选择抑制消息持久化，用于子Agent隐藏输出） */
    public ChatResponse chatSyncWithAgent(ChatRequest chatRequest, String userId, String targetAgentId,
            boolean suppressPersistence) {
        // 1. 准备以“目标Agent”为主体的对话环境（同一session）
        ChatContext environment = prepareEnvironmentWithTargetAgent(chatRequest, userId, targetAgentId);
        environment.setStreaming(false);
        environment.setSuppressPersistence(suppressPersistence);

        // 2. 获取同步传输方式
        MessageTransport<ChatResponse> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SYNC);

        // 3. 获取适合的消息处理器
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent());

        // 4. 执行
        return handler.chat(environment, transport);
    }

    /** 嵌套流式对话（指定子Agent，复用外层SSE传输） */
    public void chatStreamWithAgentNested(ChatRequest chatRequest, String userId, String targetAgentId,
            MessageTransport<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> transport) {
        ChatContext environment = prepareEnvironmentWithTargetAgent(chatRequest, userId, targetAgentId);
        environment.setStreaming(true);

        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent());
        handler.chat(environment, transport);
    }

    /** 准备以“目标Agent”为主体的对话环境（共享同一session） */
    private ChatContext prepareEnvironmentWithTargetAgent(ChatRequest chatRequest, String userId, String targetAgentId) {
        String sessionId = chatRequest.getSessionId();
        // 校验会话存在
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);

        // 目标Agent（含版本覆盖）
        AgentEntity targetAgent = getAgentWithValidation(targetAgentId, userId);

        // 工具配置
        List<String> mcpServerNames = getMcpServerNames(targetAgent.getToolIds(), userId);
        logger.debug("🔧 [子Agent工具配置] Agent: {}, ToolIds: {}, MCP服务器: {}",
            targetAgent.getName(), targetAgent.getToolIds(), mcpServerNames);

        // 模型配置（基于目标Agent的工作区）
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(targetAgentId, userId);
        LLMModelConfig llmModelConfig = workspace.getLlmModelConfig();
        ModelEntity model = getModelForChat(llmModelConfig, null, userId);

        // 高可用服务商选择
        List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(userId);
        HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId, sessionId,
                fallbackChain);
        ProviderEntity originalProvider = llmDomainService.getProvider(model.getProviderId());
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel();
        String instanceId = result.getInstanceId();
        provider.isActive();

        // 构造环境
        ChatContext chatContext = createChatContext(chatRequest, userId, targetAgent, model, selectedModel,
                originalProvider, provider, llmModelConfig, mcpServerNames, instanceId);
        setupContextAndHistory(chatContext, chatRequest);
        return chatContext;
    }

    /** 准备对话环境
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return 对话环境 */
    private ChatContext prepareEnvironment(ChatRequest chatRequest, String userId) {
        return prepareEnvironmentWithModel(chatRequest, userId, null);
    }

    /** 准备对话环境（支持指定模型）- 用于外部API
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @param modelId 指定的模型ID（可选，为null时使用Agent绑定的模型）
     * @return 对话环境 */
    private ChatContext prepareEnvironmentWithModel(ChatRequest chatRequest, String userId, String modelId) {
        // 1. 获取会话和Agent信息
        String sessionId = chatRequest.getSessionId();
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        String agentId = session.getAgentId();
        AgentEntity agent = getAgentWithValidation(agentId, userId);

        // 2. 获取工具配置
        List<String> mcpServerNames = getMcpServerNames(agent.getToolIds(), userId);

        // 3. 获取模型配置
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(agentId, userId);
        LLMModelConfig llmModelConfig = workspace.getLlmModelConfig();
        ModelEntity model = getModelForChat(llmModelConfig, modelId, userId);

        // 4. 获取高可用服务商信息
        List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(userId);
        HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId, sessionId,
                fallbackChain);
        ProviderEntity originalProvider = llmDomainService.getProvider(model.getProviderId());
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel();
        String instanceId = result.getInstanceId();
        provider.isActive();

        // 5. 创建并配置环境对象
        ChatContext chatContext = createChatContext(chatRequest, userId, agent, model, selectedModel, originalProvider,
                provider, llmModelConfig, mcpServerNames, instanceId);
        setupContextAndHistory(chatContext, chatRequest);

        return chatContext;
    }

    /** 获取Agent并进行验证 */
    private AgentEntity getAgentWithValidation(String agentId, String userId) {
        AgentEntity agent = agentDomainService.getAgentById(agentId);
        if (!agent.getUserId().equals(userId) && !agent.getEnabled()) {
            throw new BusinessException("agent已被禁用");
        }

        // 处理安装的助理版本
        if (!agent.getUserId().equals(userId)) {
            AgentVersionEntity latestAgentVersion = agentDomainService.getLatestAgentVersion(agentId);
            BeanUtils.copyProperties(latestAgentVersion, agent);
        }

        return agent;
    }

    /** 获取MCP服务器名称列表 */
    private List<String> getMcpServerNames(List<String> toolIds, String userId) {
        if (toolIds == null || toolIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserToolEntity> installTool = userToolDomainService.getInstallTool(toolIds, userId);
        return installTool.stream().map(UserToolEntity::getMcpServerName).collect(Collectors.toList());
    }

    /** 获取对话使用的模型 */
    private ModelEntity getModelForChat(LLMModelConfig llmModelConfig, String specifiedModelId, String userId) {
        String finalModelId;
        if (specifiedModelId != null && !specifiedModelId.trim().isEmpty()) {
            finalModelId = specifiedModelId;
        } else {
            finalModelId = llmModelConfig.getModelId();
        }

        ModelEntity model = llmDomainService.findModelById(finalModelId);
        if (finalModelId == null) {
            String userDefaultModelId = userSettingsDomainService.getUserDefaultModelId(userId);
            model = llmDomainService.getModelById(userDefaultModelId);
        } else if (model == null) {
            model = llmDomainService.getModelById(finalModelId);
        }
        model.isActive();
        return model;
    }

    /** 创建ChatContext对象 */
    private ChatContext createChatContext(ChatRequest chatRequest, String userId, AgentEntity agent,
            ModelEntity originalModel, ModelEntity selectedModel, ProviderEntity originalProvider,
            ProviderEntity provider, LLMModelConfig llmModelConfig, List<String> mcpServerNames, String instanceId) {
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId(chatRequest.getSessionId());
        chatContext.setUserId(userId);
        chatContext.setUserMessage(chatRequest.getMessage());
        chatContext.setAgent(agent);
        chatContext.setOriginalModel(originalModel);
        chatContext.setModel(selectedModel);
        chatContext.setOriginalProvider(originalProvider);
        chatContext.setProvider(provider);
        chatContext.setLlmModelConfig(llmModelConfig);
        chatContext.setMcpServerNames(mcpServerNames);
        chatContext.setFileUrls(chatRequest.getFileUrls());
        chatContext.setInstanceId(instanceId);
        return chatContext;
    }

    /** 设置上下文和历史消息
     *
     * @param environment 对话环境 */
    private void setupContextAndHistory(ChatContext environment, ChatRequest chatRequest) {
        String sessionId = environment.getSessionId();

        // 获取上下文
        ContextEntity contextEntity = contextDomainService.findBySessionId(sessionId);
        List<MessageEntity> messageEntities = new ArrayList<>();

        if (contextEntity != null) {
            // 获取活跃消息(包括摘要)
            List<String> activeMessageIds = contextEntity.getActiveMessages();
            messageEntities = messageDomainService.listByIds(activeMessageIds);

            // 应用Token溢出策略, 上下文历史消息以token策略返回的为准
            messageEntities = applyTokenOverflowStrategy(environment, contextEntity, messageEntities);
        } else {
            contextEntity = new ContextEntity();
            contextEntity.setSessionId(sessionId);
        }

        // 特殊处理当前对话的文件，因为在后续的对话中无法发送文件
        List<String> fileUrls = chatRequest.getFileUrls();
        if (!fileUrls.isEmpty()) {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setRole(Role.USER);
            messageEntity.setFileUrls(fileUrls);
            messageEntities.add(messageEntity);
        }

        environment.setContextEntity(contextEntity);
        environment.setMessageHistory(messageEntities);
    }

    /** 应用Token溢出策略，返回处理后的历史消息
     *
     * @param environment 对话环境
     * @param contextEntity 上下文实体
     * @param messageEntities 消息实体列表 */
    private List<MessageEntity> applyTokenOverflowStrategy(ChatContext environment, ContextEntity contextEntity,
            List<MessageEntity> messageEntities) {

        LLMModelConfig llmModelConfig = environment.getLlmModelConfig();
        ProviderEntity provider = environment.getProvider();

        // 处理Token溢出
        TokenOverflowStrategyEnum strategyType = llmModelConfig.getStrategyType();

        // Token处理
        List<TokenMessage> tokenMessages = tokenizeMessage(messageEntities);

        // 构造Token配置
        TokenOverflowConfig tokenOverflowConfig = new TokenOverflowConfig();
        tokenOverflowConfig.setStrategyType(strategyType);
        tokenOverflowConfig.setMaxTokens(llmModelConfig.getMaxTokens());
        tokenOverflowConfig.setSummaryThreshold(llmModelConfig.getSummaryThreshold());
        tokenOverflowConfig.setReserveRatio(llmModelConfig.getReserveRatio());

        // 设置提供商配置
        org.xhy.domain.llm.model.config.ProviderConfig providerConfig = provider.getConfig();
        tokenOverflowConfig.setProviderConfig(new ProviderConfig(providerConfig.getApiKey(),
                providerConfig.getBaseUrl(), environment.getModel().getModelId(), provider.getProtocol()));

        // 处理Token
        TokenProcessResult result = tokenDomainService.processMessages(tokenMessages, tokenOverflowConfig);
        List<TokenMessage> retainedMessages = new ArrayList<>(tokenMessages);
        TokenMessage newSummaryMessage = null;
        // 更新上下文
        if (result.isProcessed()) {
            retainedMessages = result.getRetainedMessages();
            // 统一对 活跃消息进行时间升序排序
            List<String> retainedMessageIds = retainedMessages.stream()
                    .sorted(Comparator.comparing(TokenMessage::getCreatedAt)).map(TokenMessage::getId)
                    .collect(Collectors.toList());
            if (strategyType == TokenOverflowStrategyEnum.SUMMARIZE
                    && retainedMessages.get(0).getRole().equals(Role.SUMMARY.name())) {
                newSummaryMessage = retainedMessages.get(0);
                contextEntity.setSummary(newSummaryMessage.getContent());
            }

            contextEntity.setActiveMessages(retainedMessageIds);
        }
        Set<String> retainedMessageIdSet = retainedMessages.stream().map(TokenMessage::getId)
                .collect(Collectors.toSet());
        // 从messageEntity中过滤出保留的消息，防止Entity字段丢失
        List<MessageEntity> newHistoryMessages = messageEntities.stream()
                .filter(message -> retainedMessageIdSet.contains(message.getId()) && !message.isSummaryMessage())
                .collect(Collectors.toList());
        if (newSummaryMessage != null) {
            newHistoryMessages.add(0, this.summaryMessageToEntity(newSummaryMessage, environment.getSessionId()));
        }
        return newHistoryMessages;
    }

    /** 消息实体转换为token消息 */
    private List<TokenMessage> tokenizeMessage(List<MessageEntity> messageEntities) {
        return messageEntities.stream().map(message -> {
            TokenMessage tokenMessage = new TokenMessage();
            tokenMessage.setId(message.getId());
            tokenMessage.setRole(message.getRole().name());
            tokenMessage.setContent(message.getContent());
            tokenMessage.setTokenCount(message.getTokenCount());
            tokenMessage.setBodyTokenCount(message.getBodyTokenCount());
            tokenMessage.setCreatedAt(message.getCreatedAt());
            return tokenMessage;
        }).collect(Collectors.toList());
    }

    private MessageEntity summaryMessageToEntity(TokenMessage tokenMessage, String sessionId) {
        MessageEntity messageEntity = new MessageEntity();
        BeanUtil.copyProperties(tokenMessage, messageEntity);
        messageEntity.setRole(Role.fromCode(tokenMessage.getRole()));
        messageEntity.setSessionId(sessionId);
        messageEntity.setMessageType(MessageType.TEXT);
        return messageEntity;
    }

    /** Agent预览功能 - 无需保存会话的对话体验
     *
     * @param previewRequest 预览请求
     * @param userId 用户ID
     * @return SSE发射器 */
    public SseEmitter previewAgent(AgentPreviewRequest previewRequest, String userId) {
        // 1. 准备预览环境
        ChatContext environment = preparePreviewEnvironment(previewRequest, userId);

        // 2. 获取传输方式
        MessageTransport<SseEmitter> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 3. 使用预览专用的消息处理器
        return previewMessageHandler.chat(environment, transport);
    }

    /** 准备预览对话环境
     *
     * @param previewRequest 预览请求
     * @param userId 用户ID
     * @return 预览对话环境 */
    private ChatContext preparePreviewEnvironment(AgentPreviewRequest previewRequest, String userId) {
        // 1. 创建虚拟Agent和获取模型
        AgentEntity virtualAgent = createVirtualAgent(previewRequest, userId);
        String modelId = getPreviewModelId(previewRequest, userId);
        ModelEntity model = getModelForChat(null, modelId, userId);

        // 2. 获取服务商信息（预览不使用高可用）
        ProviderEntity provider = llmDomainService.getProvider(model.getProviderId());
        provider.isActive();
        provider.isAvailable(provider.getUserId());
        // 3. 获取工具配置
        List<String> mcpServerNames = getMcpServerNames(previewRequest.getToolIds(), userId);

        // 4. 创建预览配置
        LLMModelConfig llmModelConfig = createDefaultLLMModelConfig(modelId);

        // 5. 创建并配置环境对象
        ChatContext chatContext = createPreviewChatContext(previewRequest, userId, virtualAgent, model, provider,
                llmModelConfig, mcpServerNames);
        setupPreviewContextAndHistory(chatContext, previewRequest);

        return chatContext;
    }

    /** 获取预览使用的模型ID */
    private String getPreviewModelId(AgentPreviewRequest previewRequest, String userId) {
        String modelId = previewRequest.getModelId();
        if (modelId == null || modelId.trim().isEmpty()) {
            modelId = userSettingsDomainService.getUserDefaultModelId(userId);
            if (modelId == null) {
                throw new BusinessException("用户未设置默认模型，且预览请求中未指定模型");
            }
        }
        return modelId;
    }

    /** 创建预览ChatContext对象 */
    private ChatContext createPreviewChatContext(AgentPreviewRequest previewRequest, String userId, AgentEntity agent,
            ModelEntity model, ProviderEntity provider, LLMModelConfig llmModelConfig, List<String> mcpServerNames) {
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId("preview-session");
        chatContext.setUserId(userId);
        chatContext.setUserMessage(previewRequest.getUserMessage());
        chatContext.setAgent(agent);
        chatContext.setModel(model);
        chatContext.setProvider(provider);
        chatContext.setLlmModelConfig(llmModelConfig);
        chatContext.setMcpServerNames(mcpServerNames);
        chatContext.setFileUrls(previewRequest.getFileUrls());
        return chatContext;
    }

    /** 创建虚拟Agent实体 */
    private AgentEntity createVirtualAgent(AgentPreviewRequest previewRequest, String userId) {
        AgentEntity virtualAgent = new AgentEntity();
        virtualAgent.setId("preview-agent");
        virtualAgent.setUserId(userId);
        virtualAgent.setName("预览助理");
        virtualAgent.setSystemPrompt(previewRequest.getSystemPrompt());
        virtualAgent.setToolIds(previewRequest.getToolIds());
        virtualAgent.setToolPresetParams(previewRequest.getToolPresetParams());
        virtualAgent.setKnowledgeBaseIds(previewRequest.getKnowledgeBaseIds()); // 设置知识库IDs用于RAG功能

        virtualAgent.setEnabled(true);
        virtualAgent.setCreatedAt(LocalDateTime.now());
        virtualAgent.setUpdatedAt(LocalDateTime.now());
        return virtualAgent;
    }

    /** 创建默认的LLM模型配置 */
    private LLMModelConfig createDefaultLLMModelConfig(String modelId) {
        LLMModelConfig llmModelConfig = new LLMModelConfig();
        llmModelConfig.setModelId(modelId);
        llmModelConfig.setTemperature(0.7);
        llmModelConfig.setTopP(0.9);
        llmModelConfig.setMaxTokens(4000);
        llmModelConfig.setStrategyType(TokenOverflowStrategyEnum.NONE);
        llmModelConfig.setSummaryThreshold(2000);
        return llmModelConfig;
    }

    /** 设置预览上下文和历史消息 */
    private void setupPreviewContextAndHistory(ChatContext environment, AgentPreviewRequest previewRequest) {
        // 创建虚拟上下文实体
        ContextEntity contextEntity = new ContextEntity();
        contextEntity.setSessionId("preview-session");
        contextEntity.setActiveMessages(new ArrayList<>());

        // 转换前端传入的历史消息为实体
        List<MessageEntity> messageEntities = new ArrayList<>();
        List<MessageDTO> messageHistory = previewRequest.getMessageHistory();
        if (messageHistory != null && !messageHistory.isEmpty()) {
            for (MessageDTO messageDTO : messageHistory) {
                MessageEntity messageEntity = new MessageEntity();
                messageEntity.setId(messageDTO.getId());
                messageEntity.setRole(messageDTO.getRole());
                messageEntity.setContent(messageDTO.getContent());
                messageEntity.setSessionId("preview-session");
                messageEntity.setCreatedAt(messageDTO.getCreatedAt());
                messageEntity.setFileUrls(messageDTO.getFileUrls());
                messageEntity.setTokenCount(messageDTO.getRole() == Role.USER ? 50 : 100); // 预估token数
                messageEntities.add(messageEntity);
            }
        }
        // 特殊处理当前对话的文件，因为在后续的对话中无法发送文件
        List<String> fileUrls = previewRequest.getFileUrls();
        if (!fileUrls.isEmpty()) {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setRole(Role.USER);
            messageEntity.setSessionId("preview-session");
            messageEntity.setFileUrls(fileUrls);
            messageEntities.add(messageEntity);
        }

        environment.setContextEntity(contextEntity);
        environment.setMessageHistory(messageEntities);
    }

    /** Widget聊天方法 - 流式响应
     *
     * @param publicId 公开访问ID
     * @param widgetChatRequest Widget聊天请求
     * @param widgetEntity Widget配置实体
     * @return SSE发射器 */
    public SseEmitter widgetChat(String publicId, WidgetChatRequest widgetChatRequest, AgentWidgetEntity widgetEntity) {
        // 1. 准备Widget对话环境
        ChatContext environment = prepareWidgetEnvironment(publicId, widgetChatRequest, widgetEntity);

        // 2. 获取传输方式
        MessageTransport<SseEmitter> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 3. 获取适合的消息处理器（传入widget参数以支持类型选择）
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent(), widgetEntity);

        // 4. 处理对话
        return handler.chat(environment, transport);
    }

    /** Widget聊天方法 - 同步响应
     *
     * @param publicId 公开访问ID
     * @param widgetChatRequest Widget聊天请求
     * @param widgetEntity Widget配置实体
     * @return 同步聊天响应 */
    public ChatResponse widgetChatSync(String publicId, WidgetChatRequest widgetChatRequest,
            AgentWidgetEntity widgetEntity) {
        // 1. 准备Widget对话环境（设置为非流式）
        ChatContext environment = prepareWidgetEnvironment(publicId, widgetChatRequest, widgetEntity);
        environment.setStreaming(false); // 设置为同步模式

        // 2. 获取同步传输方式
        MessageTransport<ChatResponse> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SYNC);

        // 3. 获取适合的消息处理器（传入widget参数以支持类型选择）
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent(), widgetEntity);

        // 4. 处理对话
        return handler.chat(environment, transport);
    }

    /** 准备Widget对话环境
     *
     * @param publicId 公开访问ID
     * @param widgetChatRequest Widget聊天请求
     * @param widgetEntity Widget配置实体
     * @return 对话环境 */
    private ChatContext prepareWidgetEnvironment(String publicId, WidgetChatRequest widgetChatRequest,
            AgentWidgetEntity widgetEntity) {
        // 检查Widget类型，如果是RAG类型则创建RAG专用上下文
        if (widgetEntity.isRagWidget()) {
            return createRagWidgetContext(publicId, widgetChatRequest, widgetEntity);
        }

        // Agent类型Widget的处理逻辑
        // 1. 获取Agent和模型信息
        String agentId = widgetEntity.getAgentId();
        String creatorUserId = widgetEntity.getUserId();
        String sessionId = widgetChatRequest.getSessionId();

        // 2. 获取Agent实体（使用创建者的权限）
        AgentEntity agent = getAgentWithValidation(agentId, creatorUserId);

        // 3. 获取Widget配置指定的模型
        ModelEntity model = llmDomainService.getModelById(widgetEntity.getModelId());

        // 4. 获取工具配置
        List<String> mcpServerNames = getMcpServerNames(agent.getToolIds(), creatorUserId);

        // 5. 获取高可用服务商信息（使用创建者的配置）
        List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(creatorUserId);
        HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, creatorUserId,
                sessionId, fallbackChain);
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel();
        String instanceId = result.getInstanceId();
        provider.isActive();

        // 6. 创建模型配置（使用默认配置）
        LLMModelConfig llmModelConfig = createDefaultLLMModelConfig(selectedModel.getModelId());

        // 7. 创建并配置环境对象
        ChatContext chatContext = createWidgetChatContext(widgetChatRequest, agent, selectedModel, provider,
                llmModelConfig, mcpServerNames, instanceId, publicId, creatorUserId);
        setupWidgetContextAndHistory(chatContext, widgetChatRequest);

        return chatContext;
    }

    /** 创建Widget ChatContext对象 */
    private ChatContext createWidgetChatContext(WidgetChatRequest widgetChatRequest, AgentEntity agent,
            ModelEntity model, ProviderEntity provider, LLMModelConfig llmModelConfig, List<String> mcpServerNames,
            String instanceId, String publicId, String creatorUserId) {
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId(widgetChatRequest.getSessionId());
        chatContext.setUserId(creatorUserId); // Widget聊天使用创建者的userId用于工具调用
        chatContext.setUserMessage(widgetChatRequest.getMessage());
        chatContext.setAgent(agent);
        chatContext.setModel(model);
        chatContext.setProvider(provider);
        chatContext.setLlmModelConfig(llmModelConfig);
        chatContext.setMcpServerNames(mcpServerNames);
        chatContext.setFileUrls(widgetChatRequest.getFileUrls());
        chatContext.setInstanceId(instanceId);
        // 标记为公开访问Widget模式
        chatContext.setPublicAccess(true);
        chatContext.setPublicId(publicId);
        return chatContext;
    }

    /** 设置Widget上下文和历史消息 */
    private void setupWidgetContextAndHistory(ChatContext environment, WidgetChatRequest widgetChatRequest) {
        String sessionId = environment.getSessionId();

        // 获取或创建匿名会话的上下文
        ContextEntity contextEntity = contextDomainService.findBySessionId(sessionId);
        List<MessageEntity> messageEntities = new ArrayList<>();

        if (contextEntity != null) {
            // 获取活跃消息
            List<String> activeMessageIds = contextEntity.getActiveMessages();
            messageEntities = messageDomainService.listByIds(activeMessageIds);

            // 对于Widget聊天，暂不应用复杂的Token溢出策略，使用简单的窗口限制
            if (messageEntities.size() > 20) { // 限制历史消息数量
                messageEntities = messageEntities.subList(Math.max(0, messageEntities.size() - 20),
                        messageEntities.size());
            }
        } else {
            contextEntity = new ContextEntity();
            contextEntity.setSessionId(sessionId);
        }

        // 处理当前对话的文件
        List<String> fileUrls = widgetChatRequest.getFileUrls();
        if (!fileUrls.isEmpty()) {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setRole(Role.USER);
            messageEntity.setFileUrls(fileUrls);
            messageEntities.add(messageEntity);
        }

        environment.setContextEntity(contextEntity);
        environment.setMessageHistory(messageEntities);
    }

    // ========== RAG 支持方法 ==========

    /** RAG流式问答 - 基于数据集
     * @param request RAG流式聊天请求
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChat(RagStreamChatRequest request, String userId) {
        // 1. 创建临时RAG会话
        String sessionId = ragSessionManager.createOrGetRagSession(userId);

        // 2. 转换为RagChatRequest
        RagChatRequest ragChatRequest = RagChatRequest.fromRagStreamChatRequest(request, sessionId);

        // 3. 使用通用的chat入口
        return chat(ragChatRequest, userId);
    }

    /** RAG流式问答 - 基于已安装知识库
     * @param request RAG流式聊天请求
     * @param userRagId 用户RAG ID
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChatByUserRag(RagStreamChatRequest request, String userRagId, String userId) {
        // 1. 创建用户RAG专用会话
        String sessionId = ragSessionManager.createOrGetUserRagSession(userId, userRagId);

        // 2. 转换为RagChatRequest（包含userRagId）
        RagChatRequest ragChatRequest = RagChatRequest.fromRagStreamChatRequestWithUserRag(request, userRagId,
                sessionId);

        // 3. 使用通用的chat入口
        return chat(ragChatRequest, userId);
    }

    /** 根据请求类型准备环境
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return 聊天上下文 */
    private ChatContext prepareEnvironmentByRequestType(ChatRequest chatRequest, String userId) {
        if (chatRequest instanceof RagChatRequest) {
            return prepareRagEnvironment((RagChatRequest) chatRequest, userId);
        }

        // 标准对话环境准备
        return prepareEnvironment(chatRequest, userId);
    }

    /** 准备RAG环境
     * @param ragRequest RAG聊天请求
     * @param userId 用户ID
     * @return RAG聊天上下文 */
    private RagChatContext prepareRagEnvironment(RagChatRequest ragRequest, String userId) {
        // 1. 获取会话上下文和历史消息
        String sessionId = ragRequest.getSessionId();
        ContextEntity contextEntity = contextDomainService.findBySessionId(sessionId);
        List<MessageEntity> messageHistory = new ArrayList<>();

        if (contextEntity != null && contextEntity.getActiveMessages() != null) {
            messageHistory = messageDomainService.listByIds(contextEntity.getActiveMessages());
        } else {
            contextEntity = new ContextEntity();
            contextEntity.setSessionId(sessionId);
        }

        // 2. 创建RAG专用Agent
        AgentEntity ragAgent = createRagAgent();

        // 3. 获取用户默认模型配置
        String userDefaultModelId = userSettingsDomainService.getUserDefaultModelId(userId);
        ModelEntity model = llmDomainService.getModelById(userDefaultModelId);
        List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(userId);

        // 4. 获取高可用服务商
        HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId, sessionId,
                fallbackChain);
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel();

        // 5. 构建RAG上下文
        RagChatContext ragContext = new RagChatContext();
        ragContext.setSessionId(sessionId);
        ragContext.setUserId(userId);
        ragContext.setUserMessage(ragRequest.getMessage());
        ragContext.setRagSearchRequest(ragRequest.toRagSearchRequest());
        ragContext.setUserRagId(ragRequest.getUserRagId());
        ragContext.setFileId(ragRequest.getFileId());
        ragContext.setAgent(ragAgent);
        ragContext.setModel(selectedModel);
        ragContext.setProvider(provider);
        ragContext.setInstanceId(result.getInstanceId());
        ragContext.setContextEntity(contextEntity);
        ragContext.setMessageHistory(messageHistory);
        ragContext.setStreaming(true);
        ragContext.setFileUrls(ragRequest.getFileUrls());

        return ragContext;
    }

    /** 创建RAG专用的虚拟Agent
     * @return RAG Agent */
    private AgentEntity createRagAgent() {
        AgentEntity ragAgent = new AgentEntity();
        ragAgent.setId("system-rag-agent");
        ragAgent.setUserId("system");
        ragAgent.setName("RAG助手");
        ragAgent.setSystemPrompt("""
                你是一位专业的文档问答助手，专门基于提供的文档内容回答用户问题。
                你的回答应该准确、有帮助，并且要诚实地告知用户当文档中没有相关信息时的情况。
                请遵循以下原则：
                1. 优先基于提供的文档内容回答
                2. 如果文档中没有相关信息，请明确告知用户
                3. 使用清晰的Markdown格式组织回答
                4. 在适当的地方引用文档页码或来源
                """);
        ragAgent.setEnabled(true);
        return ragAgent;
    }

    /** 创建RAG Widget专用的上下文
     * @param publicId 公开访问ID
     * @param widgetChatRequest Widget聊天请求
     * @param widgetEntity Widget配置实体
     * @return RAG聊天上下文 */
    private ChatContext createRagWidgetContext(String publicId, WidgetChatRequest widgetChatRequest,
            AgentWidgetEntity widgetEntity) {
        // 1. 获取基础信息
        String creatorUserId = widgetEntity.getUserId();
        String sessionId = widgetChatRequest.getSessionId();

        // 2. 创建系统RAG Agent（用于RAG对话）
        AgentEntity ragAgent = createRagAgent();

        // 3. 获取Widget配置指定的模型
        ModelEntity model = llmDomainService.getModelById(widgetEntity.getModelId());

        // 4. 获取高可用服务商信息
        List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(creatorUserId);
        HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, creatorUserId,
                sessionId, fallbackChain);
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel();
        String instanceId = result.getInstanceId();
        provider.isActive();

        // 5. 创建模型配置
        LLMModelConfig llmModelConfig = createDefaultLLMModelConfig(selectedModel.getModelId());

        // 6. 创建RAG搜索请求
        RagSearchRequest ragSearchRequest = new RagSearchRequest();
        ragSearchRequest.setQuestion(widgetChatRequest.getMessage());
        ragSearchRequest.setDatasetIds(widgetEntity.getKnowledgeBaseIds()); // 使用Widget配置的知识库ID
        ragSearchRequest.setMaxResults(5); // 默认检索5个结果
        ragSearchRequest.setMinScore(0.7); // 默认最小相似度
        ragSearchRequest.setEnableRerank(true); // 默认启用重排序

        // 7. 创建RAG专用上下文
        RagChatContext ragContext = new RagChatContext();
        ragContext.setSessionId(sessionId);
        ragContext.setUserId(creatorUserId);
        ragContext.setUserMessage(widgetChatRequest.getMessage());
        ragContext.setAgent(ragAgent);
        ragContext.setModel(selectedModel);
        ragContext.setProvider(provider);
        ragContext.setLlmModelConfig(llmModelConfig);
        ragContext.setInstanceId(instanceId);
        ragContext.setRagSearchRequest(ragSearchRequest);
        ragContext.setUserRagId(null); // Widget RAG使用数据集ID，不使用userRagId
        ragContext.setFileUrls(widgetChatRequest.getFileUrls());

        // 8. 设置会话和上下文
        setupWidgetContextAndHistory(ragContext, widgetChatRequest);

        return ragContext;
    }

}
