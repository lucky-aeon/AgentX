package org.xhy.application.conversation.service;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.internal.chat.AssistantMessage;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.agent.service.AgentWorkspaceDomainService;
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
import org.xhy.domain.llm.service.LlmDomainService;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.token.service.TokenDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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


    public ConversationAppService(
            ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService, AgentDomainService agentDomainService, AgentWorkspaceDomainService agentWorkspaceDomainService, LlmDomainService llmDomainService, ContextDomainService contextDomainService, TokenDomainService tokenDomainService, MessageDomainService messageDomainService) {
        this.conversationDomainService = conversationDomainService;
        this.sessionDomainService = sessionDomainService;
        this.agentDomainService = agentDomainService;
        this.agentWorkspaceDomainService = agentWorkspaceDomainService;
        this.llmDomainService = llmDomainService;
        this.contextDomainService = contextDomainService;
        this.tokenDomainService = tokenDomainService;
        this.messageDomainService = messageDomainService;
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


    public SseEmitter chat(ChatRequest chatRequest, String userId) {

        // 获取会话
        String sessionId = chatRequest.getSessionId();
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        String agentId = session.getAgentId();

        // 获取对应agent是否可以使用：如果 userId 不同并且是禁用，则不可对话
        AgentEntity agent = agentDomainService.getAgentById(agentId);
        if (!agent.getUserId().equals(userId) && !agent.getEnabled()) {
            throw new BusinessException("agent已被禁用");
        }

        // 从工作区中获取对应的模型信息
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(agentId, userId);
        LLMModelConfig llmModelConfig = workspace.getLlmModelConfig();
        String modelId = llmModelConfig.getModelId();
        ModelEntity model = llmDomainService.getModelById(modelId);

        model.isActive();

        // 获取服务商信息
        ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
        provider.isActive();


        org.xhy.domain.llm.model.config.ProviderConfig config = provider.getConfig();

        ProviderConfig providerConfig = new ProviderConfig(config.getApiKey(), config.getBaseUrl(), model.getModelId(), provider.getProtocol());

        StreamingChatLanguageModel chatStreamClient = LLMProviderService.getStream(provider.getProtocol(), providerConfig);


        // 消息列表
        ContextEntity contextEntity = contextDomainService.findBySessionId(sessionId);
        List<MessageEntity> messageEntities = new ArrayList<>();
        if (contextEntity != null) {
            TokenOverflowStrategyEnum strategyType = llmModelConfig.getStrategyType();

            // 根据消息上下文获取消息列表
            List<String> activeMessagesIds = contextEntity.getActiveMessages();
            messageEntities = messageDomainService.listByIds(activeMessagesIds);

            // 尝试触发 token 策略
            List<TokenMessage> tokenMessages = this.tokenizeMessage(messageEntities);

            TokenOverflowConfig tokenOverflowConfig = new TokenOverflowConfig();
            tokenOverflowConfig.setStrategyType(strategyType);
            tokenOverflowConfig.setMaxTokens(llmModelConfig.getMaxTokens());
            tokenOverflowConfig.setSummaryThreshold(llmModelConfig.getSummaryThreshold());
            tokenOverflowConfig.setProviderConfig(providerConfig);
            TokenProcessResult tokenProcessResult = tokenDomainService.processMessages(tokenMessages, tokenOverflowConfig);

            if (tokenProcessResult.isProcessed()) {
                // 保留后的消息列表
                List<TokenMessage> retainedMessages = tokenProcessResult.getRetainedMessages();
                List<String> retainedMessageIds = retainedMessages.stream().map(TokenMessage::getId).collect(Collectors.toList());
                if (strategyType == TokenOverflowStrategyEnum.SUMMARIZE) {
                    String newSummary = tokenProcessResult.getSummary();
                    String oldSummary = contextEntity.getSummary();
                    contextEntity.setSummary(oldSummary + newSummary);
                }
                contextEntity.setActiveMessages(retainedMessageIds);
            }
        } else {
            contextEntity = new ContextEntity();
            contextEntity.setSessionId(sessionId);
        }


        // 用户消息
        MessageEntity userMessageEntity = new MessageEntity();
        userMessageEntity.setRole(Role.USER);
        userMessageEntity.setContent(chatRequest.getMessage());
        userMessageEntity.setSessionId(sessionId);

        // 大模型消息
        MessageEntity llmMessageEntity = new MessageEntity();
        llmMessageEntity.setRole(Role.SYSTEM);
        llmMessageEntity.setSessionId(sessionId);
        llmMessageEntity.setModel(model.getModelId());
        llmMessageEntity.setProvider(provider.getId());

        // 创建 SseEmitter 实例，设置更长的超时时间 (5分钟)
        SseEmitter emitter = new SseEmitter(300000L);

        // 添加超时回调
        emitter.onTimeout(() -> {
            try {
                StreamChatResponse response = new StreamChatResponse();
                response.setContent("\n\n[系统提示：响应超时，请重试]");
                response.setDone(true);
                emitter.send(response);
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 添加错误回调
        emitter.onError((ex) -> {
            try {
                StreamChatResponse response = new StreamChatResponse();
                response.setContent("\n\n[系统错误：" + ex.getMessage() + "]");
                response.setDone(true);
                emitter.send(response);
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // messageEntities 转为 ChatRequest
        List<ChatMessage> chatMessages = new ArrayList<>();
        dev.langchain4j.model.chat.request.ChatRequest.Builder chatRequestBuilder = new dev.langchain4j.model.chat.request.ChatRequest.Builder();

        List<Content> userContents = new ArrayList<>();
        List<Content> systemContents = new ArrayList<>();
        for (MessageEntity messageEntity : messageEntities) {
            Role role = messageEntity.getRole();
            String content = messageEntity.getContent();
            if (role == Role.USER) {
                userContents.add(new TextContent(content));
            } else if (role == Role.SYSTEM) {
                systemContents.add(new TextContent(content));
            }
        }
        // 添加摘要消息
        if (StringUtils.isNotEmpty(contextEntity.getSummary())) {
            String preStr = "以下消息是用户之前的历史消息精炼成的摘要消息";
            chatMessages.add(new AiMessage(preStr + contextEntity.getSummary()));
        }

        // 添加历史消息
        userContents.add(new TextContent(chatRequest.getMessage()));
        chatMessages.add(new UserMessage(userContents));
        if (StringUtils.isNotEmpty(agent.getSystemPrompt())) {
            chatMessages.add(new SystemMessage(agent.getSystemPrompt()));
        }

        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(model.getModelId());
        parameters.topP(llmModelConfig.getTopP()).temperature(llmModelConfig.getTemperature());
        chatRequestBuilder.messages(chatMessages);
        chatRequestBuilder.parameters(parameters.build());

        dev.langchain4j.model.chat.request.ChatRequest llmChatRequest = chatRequestBuilder.build();

        ContextEntity finalContextEntity = contextEntity;
        chatStreamClient.doChat(llmChatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent(partialResponse);
                    response.setDone(false);
                    response.setProvider(provider.getName());
                    response.setModel(model.getModelId());
                    emitter.send(response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();

                    Integer inputTokenCount = tokenUsage.inputTokenCount();
                    userMessageEntity.setTokenCount(inputTokenCount);
                    Integer outputTokenCount = tokenUsage.outputTokenCount();
                    llmMessageEntity.setTokenCount(outputTokenCount);
                    llmMessageEntity.setContent(completeResponse.aiMessage().text());
                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent("");
                    response.setDone(true);
                    response.setProvider(provider.getName());
                    response.setModel(model.getModelId());
                    emitter.send(response);
                    emitter.complete();

                    conversationDomainService.insertBathMessage(Arrays.asList(userMessageEntity, llmMessageEntity));
                    List<String> activeMessages = finalContextEntity.getActiveMessages();
                    activeMessages.add(userMessageEntity.getId());
                    activeMessages.add(llmMessageEntity.getId());
                    contextDomainService.insertOrUpdate(finalContextEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable error) {
                try {
                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent(error.getMessage());
                    response.setDone(true);
                    emitter.send(response);
                    emitter.complete();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return emitter;
    }

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