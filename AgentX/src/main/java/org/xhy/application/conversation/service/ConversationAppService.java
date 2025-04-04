package org.xhy.application.conversation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.domain.agent.service.AgentValidator;
import org.xhy.domain.agent.service.ModelProviderFacade;
import org.xhy.domain.conversation.factory.MessageFactory;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.ContextProcessor;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.model.LLMRequest;
import org.xhy.domain.llm.service.LLMRequestService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.service.StreamResponseHandler;
import org.xhy.infrastructure.sse.SseEmitterFactory;

import java.util.List;

/**
 * 对话应用服务，用于适配域层的对话服务
 */
@Service
public class ConversationAppService {

    private final ConversationDomainService conversationDomainService;
    private final SessionDomainService sessionDomainService;
    private final ContextProcessor contextProcessor;
    private final AgentValidator agentValidator;
    private final ModelProviderFacade modelProviderFacade;
    private final MessageFactory messageFactory;
    private final SseEmitterFactory sseEmitterFactory;
    private final LLMRequestService llmRequestService;
    private final StreamResponseHandler streamResponseHandler;
    private final ChatCompletionCallbackAdapter completionCallbackAdapter;

    public ConversationAppService(
            ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService,
            ContextProcessor contextProcessor,
            AgentValidator agentValidator,
            ModelProviderFacade modelProviderFacade,
            MessageFactory messageFactory,
            SseEmitterFactory sseEmitterFactory,
            LLMRequestService llmRequestService,
            StreamResponseHandler streamResponseHandler,
            ChatCompletionCallbackAdapter completionCallbackAdapter) {
        this.conversationDomainService = conversationDomainService;
        this.sessionDomainService = sessionDomainService;
        this.contextProcessor = contextProcessor;
        this.agentValidator = agentValidator;
        this.modelProviderFacade = modelProviderFacade;
        this.messageFactory = messageFactory;
        this.sseEmitterFactory = sseEmitterFactory;
        this.llmRequestService = llmRequestService;
        this.streamResponseHandler = streamResponseHandler;
        this.completionCallbackAdapter = completionCallbackAdapter;
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
        if (sessionDomainService.find(sessionId, userId) == null) {
            throw new BusinessException("会话不存在");
        }

        List<MessageEntity> conversationMessages = conversationDomainService.getConversationMessages(sessionId);
        return MessageAssembler.toDTOs(conversationMessages);
    }

    /**
     * 聊天接口
     *
     * @param chatRequest 聊天请求
     * @param userId 用户ID
     * @return SseEmitter实例
     */
    public SseEmitter chat(ChatRequest chatRequest, String userId) {
        // 1. 验证会话和Agent
        AgentValidator.ValidationResult validationResult = agentValidator.validateSessionAndAgent(
                chatRequest.getSessionId(), userId);
        
        // 2. 获取模型和提供商
        ModelProviderFacade.ModelProviderResult modelProviderResult = modelProviderFacade.getModelAndProvider(
                validationResult.getAgentEntity().getId(), userId);
        
        // 3. 处理上下文和消息
        ContextProcessor.ContextResult contextResult = contextProcessor.processContext(
                chatRequest.getSessionId(),
                modelProviderResult.getLlmModelConfig().getMaxTokens(),
                modelProviderResult.getLlmModelConfig().getStrategyType(),
                modelProviderResult.getLlmModelConfig().getSummaryThreshold(),
                modelProviderResult.getProviderConfig());
        
        // 4. 创建用户和LLM消息实体
        MessageEntity userMessageEntity = messageFactory.createUserMessage(
                chatRequest.getMessage(), 
                chatRequest.getSessionId());
        
        MessageEntity llmMessageEntity = messageFactory.createSystemMessage(
                chatRequest.getSessionId(),
                modelProviderResult.getModelEntity().getModelId(),
                modelProviderResult.getProviderEntity().getId());
        
        // 5. 创建SSE发射器
        SseEmitter emitter = sseEmitterFactory.createEmitter(300000L);
        
        try {
            // 6. 转换温度和topP参数
            float temperature = modelProviderResult.getLlmModelConfig().getTemperature().floatValue();
            float topP = modelProviderResult.getLlmModelConfig().getTopP().floatValue();
            
            // 7. 构建LLM请求
            LLMRequest llmRequest = llmRequestService.buildRequest(
                    contextResult,
                    chatRequest.getMessage(),
                    validationResult.getAgentEntity().getSystemPrompt(),
                    modelProviderResult.getModelEntity().getModelId(),
                    temperature,
                    topP);
            
            // 8. 初始化回调适配器
            completionCallbackAdapter.initialize(
                    emitter,
                    userMessageEntity,
                    llmMessageEntity,
                    contextResult.getContextEntity(),
                    modelProviderResult.getProviderEntity().getName(),
                    modelProviderResult.getModelEntity().getModelId());
            
            // 9. 执行流式请求，使用回调接口处理响应
            streamResponseHandler.handleStreamResponse(
                    modelProviderResult.getChatStreamClient(),
                    llmRequest,
                    completionCallbackAdapter);
            
            return emitter;
        } catch (Exception e) {
            // 统一错误处理
            try {
                sseEmitterFactory.sendErrorResponse(emitter, "聊天请求处理失败: " + e.getMessage());
            } finally {
                emitter.complete();
            }
            throw new BusinessException("聊天请求处理失败", e);
        }
    }
}