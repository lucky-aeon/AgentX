package org.xhy.application.conversation.service;

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
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.preview.PreviewMessageHandler;
import org.xhy.domain.user.service.UserSettingsDomainService;

import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
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
import java.util.List;
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

    public ConversationAppService(ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService, AgentDomainService agentDomainService,
            AgentWorkspaceDomainService agentWorkspaceDomainService, LLMDomainService llmDomainService,
            ContextDomainService contextDomainService, TokenDomainService tokenDomainService,
            MessageDomainService messageDomainService, MessageHandlerFactory messageHandlerFactory,
            MessageTransportFactory transportFactory, UserToolDomainService toolDomainService,
            UserSettingsDomainService userSettingsDomainService, PreviewMessageHandler previewMessageHandler,
            HighAvailabilityDomainService highAvailabilityDomainService) {
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

    /** 对话方法 - 统一入口
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return SSE发射器 */
    public SseEmitter chat(ChatRequest chatRequest, String userId) {
        // 1. 准备对话环境
        ChatContext environment = prepareEnvironment(chatRequest, userId);

        // 2. 获取传输方式 (当前仅支持SSE，将来支持WebSocket)
        MessageTransport<SseEmitter> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 3. 获取适合的消息处理器 (根据agent类型)
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent());

        // 4. 处理对话
        return handler.chat(environment, transport);
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
        return handler.chat(environment, transport);
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
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel();
        String instanceId = result.getInstanceId();
        provider.isActive();

        // 5. 创建并配置环境对象
        ChatContext chatContext = createChatContext(chatRequest, userId, agent, selectedModel, provider, llmModelConfig,
                mcpServerNames, instanceId);
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
    private ChatContext createChatContext(ChatRequest chatRequest, String userId, AgentEntity agent, ModelEntity model,
            ProviderEntity provider, LLMModelConfig llmModelConfig, List<String> mcpServerNames, String instanceId) {
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId(chatRequest.getSessionId());
        chatContext.setUserId(userId);
        chatContext.setUserMessage(chatRequest.getMessage());
        chatContext.setAgent(agent);
        chatContext.setModel(model);
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
            // 获取活跃消息
            List<String> activeMessageIds = contextEntity.getActiveMessages();
            messageEntities = messageDomainService.listByIds(activeMessageIds);

            // 应用Token溢出策略
            applyTokenOverflowStrategy(environment, contextEntity, messageEntities);
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

    /** 应用Token溢出策略
     *
     * @param environment 对话环境
     * @param contextEntity 上下文实体
     * @param messageEntities 消息实体列表 */
    private void applyTokenOverflowStrategy(ChatContext environment, ContextEntity contextEntity,
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

        // 设置提供商配置
        org.xhy.domain.llm.model.config.ProviderConfig providerConfig = provider.getConfig();
        tokenOverflowConfig.setProviderConfig(new ProviderConfig(providerConfig.getApiKey(),
                providerConfig.getBaseUrl(), environment.getModel().getModelId(), provider.getProtocol()));

        // 处理Token
        TokenProcessResult result = tokenDomainService.processMessages(tokenMessages, tokenOverflowConfig);

        // 更新上下文
        if (result.isProcessed()) {
            List<TokenMessage> retainedMessages = result.getRetainedMessages();
            List<String> retainedMessageIds = retainedMessages.stream().map(TokenMessage::getId)
                    .collect(Collectors.toList());

            if (strategyType == TokenOverflowStrategyEnum.SUMMARIZE) {
                String newSummary = result.getSummary();
                String oldSummary = contextEntity.getSummary();
                contextEntity.setSummary(oldSummary + newSummary);
            }

            contextEntity.setActiveMessages(retainedMessageIds);
        }
    }

    /** 消息实体转换为token消息 */
    private List<TokenMessage> tokenizeMessage(List<MessageEntity> messageEntities) {
        return messageEntities.stream().map(message -> {
            TokenMessage tokenMessage = new TokenMessage();
            tokenMessage.setId(message.getId());
            tokenMessage.setRole(message.getRole().name());
            tokenMessage.setContent(message.getContent());
            tokenMessage.setTokenCount(message.getTokenCount());
            tokenMessage.setCreatedAt(message.getCreatedAt());
            tokenMessage.setBodyTokenCount(message.getBodyTokenCount());
            return tokenMessage;
        }).collect(Collectors.toList());
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
}