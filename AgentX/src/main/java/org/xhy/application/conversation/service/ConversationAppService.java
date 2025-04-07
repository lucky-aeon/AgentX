package org.xhy.application.conversation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.agent.service.AgentWorkspaceDomainService;
import org.xhy.domain.conversation.handler.ChatEnvironment;
import org.xhy.domain.conversation.handler.MessageHandler;
import org.xhy.domain.conversation.handler.MessageHandlerFactory;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LlmDomainService;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.token.service.TokenDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.transport.MessageTransport;
import org.xhy.infrastructure.transport.MessageTransportFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话应用服务，用于适配域层的对话服务
 */
@Service
public class ConversationAppService {

    private final ConversationDomainService conversationDomainService;
    private final SessionDomainService sessionDomainService;
    private final AgentDomainService agentDomainService;
    private final AgentWorkspaceDomainService agentWorkspaceDomainService;
    private final LlmDomainService llmDomainService;
    private final ContextDomainService contextDomainService;
    private final TokenDomainService tokenDomainService;
    private final MessageDomainService messageDomainService;

    // 新增依赖
    private final MessageHandlerFactory messageHandlerFactory;
    private final MessageTransportFactory transportFactory;


    public ConversationAppService(
            ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService,
            AgentDomainService agentDomainService,
            AgentWorkspaceDomainService agentWorkspaceDomainService,
            LlmDomainService llmDomainService,
            ContextDomainService contextDomainService,
            TokenDomainService tokenDomainService,
            MessageDomainService messageDomainService,
            MessageHandlerFactory messageHandlerFactory,
            MessageTransportFactory transportFactory,
            LLMServiceFactory llmServiceFactory) {
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
    }

    /**
     * 获取会话中的消息列表
     *
     * @param sessionId 会话id
     * @param userId    用户id
     * @return 消息列表
     */
    public List<MessageDTO> getConversationMessages(String sessionId, String userId) {
        // 查询对应会话是否存在
        SessionEntity sessionEntity = sessionDomainService.find(sessionId, userId);

        if (sessionEntity == null) {
            throw new BusinessException("会话不存在");
        }

        List<MessageEntity> conversationMessages = conversationDomainService.getConversationMessages(sessionId);
        return MessageAssembler.toDTOs(conversationMessages);
    }

    /**
     * 对话方法 - 统一入口
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return SSE发射器
     */
    public SseEmitter chat(ChatRequest chatRequest, String userId) {
        // 1. 准备对话环境
        ChatEnvironment environment = prepareEnvironment(chatRequest, userId);

        // 2. 获取传输方式 (当前仅支持SSE，将来支持WebSocket)
        MessageTransport<SseEmitter> transport = transportFactory.getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 3. 获取适合的消息处理器 (根据agent类型)
        MessageHandler handler = messageHandlerFactory.getHandler(environment.getAgent());

        // 4. 处理对话
        return handler.handleChat(environment, transport);
    }

    /**
     * 准备对话环境
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return 对话环境
     */
    private ChatEnvironment prepareEnvironment(ChatRequest chatRequest, String userId) {
        // 1. 获取会话
        String sessionId = chatRequest.getSessionId();
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        String agentId = session.getAgentId();

        // 2. 获取对应agent
        AgentEntity agent = agentDomainService.getAgentById(agentId);
        if (!agent.getUserId().equals(userId) && !agent.getEnabled()) {
            throw new BusinessException("agent已被禁用");
        }

        // 3. 获取工作区和模型配置
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(agentId, userId);
        LLMModelConfig llmModelConfig = workspace.getLlmModelConfig();
        String modelId = llmModelConfig.getModelId();
        ModelEntity model = llmDomainService.getModelById(modelId);
        model.isActive();

        // 4. 获取服务商信息
        ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
        provider.isActive();

        // 5. 创建环境对象
        ChatEnvironment environment = new ChatEnvironment();
        environment.setSessionId(sessionId);
        environment.setUserId(userId);
        environment.setUserMessage(chatRequest.getMessage());
        environment.setAgent(agent);
        environment.setModel(model);
        environment.setProvider(provider);
        environment.setLlmModelConfig(llmModelConfig);

        // 6. 设置上下文信息和消息历史
        setupContextAndHistory(environment);

        return environment;
    }

    /**
     * 设置上下文和历史消息
     *
     * @param environment 对话环境
     */
    private void setupContextAndHistory(ChatEnvironment environment) {
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

        environment.setContextEntity(contextEntity);
        environment.setMessageHistory(messageEntities);
    }

    /**
     * 应用Token溢出策略
     *
     * @param environment 对话环境
     * @param contextEntity 上下文实体
     * @param messageEntities 消息实体列表
     */
    private void applyTokenOverflowStrategy(
            ChatEnvironment environment,
            ContextEntity contextEntity,
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
        tokenOverflowConfig.setProviderConfig(new ProviderConfig(
                providerConfig.getApiKey(),
                providerConfig.getBaseUrl(),
                environment.getModel().getModelId(),
                provider.getProtocol()));

        // 处理Token
        TokenProcessResult result = tokenDomainService.processMessages(tokenMessages, tokenOverflowConfig);

        // 更新上下文
        if (result.isProcessed()) {
            List<TokenMessage> retainedMessages = result.getRetainedMessages();
            List<String> retainedMessageIds = retainedMessages.stream()
                    .map(TokenMessage::getId)
                    .collect(Collectors.toList());

            if (strategyType == TokenOverflowStrategyEnum.SUMMARIZE) {
                String newSummary = result.getSummary();
                String oldSummary = contextEntity.getSummary();
                contextEntity.setSummary(oldSummary + newSummary);
            }

            contextEntity.setActiveMessages(retainedMessageIds);
        }
    }

    /**
     * 消息实体转换为token消息
     */
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
}