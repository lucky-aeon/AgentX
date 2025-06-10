package org.xhy.application.conversation.service;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.AgentPreviewRequest;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.application.conversation.service.message.preview.PreviewMessageHandler;
import org.xhy.application.user.service.UserSettingsAppService;

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
    private final UserSettingsAppService userSettingsAppService;
    private final PreviewMessageHandler previewMessageHandler;
    private final HighAvailabilityDomainService highAvailabilityDomainService;

    public ConversationAppService(ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService, AgentDomainService agentDomainService,
            AgentWorkspaceDomainService agentWorkspaceDomainService, LLMDomainService llmDomainService,
            ContextDomainService contextDomainService, TokenDomainService tokenDomainService,
            MessageDomainService messageDomainService, MessageHandlerFactory messageHandlerFactory,
            MessageTransportFactory transportFactory, UserToolDomainService toolDomainService,
            UserSettingsAppService userSettingsAppService, PreviewMessageHandler previewMessageHandler,
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
        this.userSettingsAppService = userSettingsAppService;
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

    /** 准备对话环境
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return 对话环境 */
    private ChatContext prepareEnvironment(ChatRequest chatRequest, String userId) {
        // 1. 获取会话
        String sessionId = chatRequest.getSessionId();
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        String agentId = session.getAgentId();

        // 2. 获取对应agent
        AgentEntity agent = agentDomainService.getAgentById(agentId);
        if (!agent.getUserId().equals(userId) && !agent.getEnabled()) {
            throw new BusinessException("agent已被禁用");
        }

        List<String> toolIds = agent.getToolIds();

        // 在工作区中的助理会分为用户自己创建的和安装的助理，因此需要区分 agent，如果 agent 的 userId 等于当前用户则使用 agent，反之使用
        // agent_version
        if (!agent.getUserId().equals(userId)) {
            AgentVersionEntity latestAgentVersion = agentDomainService.getLatestAgentVersion(agentId);
            // 直接转换即可
            toolIds = latestAgentVersion.getToolIds();
            BeanUtils.copyProperties(latestAgentVersion, agent);
        }

        // 校验工具的可用性
        List<UserToolEntity> installTool = userToolDomainService.getInstallTool(toolIds, userId);

        // 获取 mcp server name
        List<String> mcpServerNames = installTool.stream().map(UserToolEntity::getMcpServerName).toList();

        // 3. 获取工作区和模型配置
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(agentId, userId);
        LLMModelConfig llmModelConfig = workspace.getLlmModelConfig();
        String modelId = llmModelConfig.getModelId();
        ModelEntity model = llmDomainService.getModelById(modelId);
        model.isActive();

        // 4. 获取服务商信息（支持高可用和会话亲和性）
        HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId, sessionId);
        ProviderEntity provider = result.getProvider();
        ModelEntity selectedModel = result.getModel(); // 可能是不同的部署名称
        String instanceId = result.getInstanceId(); // 获取实例ID
        provider.isActive();

        // 5. 创建环境对象
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId(sessionId);
        chatContext.setUserId(userId);
        chatContext.setUserMessage(chatRequest.getMessage());
        chatContext.setAgent(agent);
        chatContext.setModel(selectedModel); // 使用高可用选择的模型（可能有不同的部署名称）
        chatContext.setProvider(provider);
        chatContext.setLlmModelConfig(llmModelConfig);
        chatContext.setMcpServerNames(mcpServerNames);
        chatContext.setFileUrls(chatRequest.getFileUrls());
        chatContext.setInstanceId(instanceId); // 设置实例ID
        // 6. 设置上下文信息和消息历史
        setupContextAndHistory(chatContext, chatRequest);

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
        // 1. 创建虚拟Agent实体
        AgentEntity virtualAgent = createVirtualAgent(previewRequest, userId);

        // 2. 获取模型信息
        String modelId = previewRequest.getModelId();
        if (modelId == null || modelId.trim().isEmpty()) {
            // 使用用户默认模型
            modelId = userSettingsAppService.getUserDefaultModelId(userId);
            if (modelId == null) {
                throw new BusinessException("用户未设置默认模型，且预览请求中未指定模型");
            }
        }

        ModelEntity model = llmDomainService.getModelById(modelId);
        model.isActive();

        // 3. 获取服务商信息
        ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
        provider.isActive();

        // 4. 处理工具配置
        List<String> toolIds = previewRequest.getToolIds();
        List<String> mcpServerNames = new ArrayList<>();
        if (toolIds != null && !toolIds.isEmpty()) {
            List<UserToolEntity> installTool = userToolDomainService.getInstallTool(toolIds, userId);
            mcpServerNames = installTool.stream().map(UserToolEntity::getMcpServerName).toList();
        }

        // 5. 创建默认的LLM模型配置
        LLMModelConfig llmModelConfig = createDefaultLLMModelConfig(modelId);

        // 6. 创建环境对象
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId("preview-session"); // 虚拟会话ID
        chatContext.setUserId(userId);
        chatContext.setUserMessage(previewRequest.getUserMessage());
        chatContext.setAgent(virtualAgent);
        chatContext.setModel(model);
        chatContext.setProvider(provider);
        chatContext.setLlmModelConfig(llmModelConfig);
        chatContext.setMcpServerNames(mcpServerNames);
        chatContext.setFileUrls(previewRequest.getFileUrls());

        // 7. 设置预览上下文和历史消息
        setupPreviewContextAndHistory(chatContext, previewRequest);

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