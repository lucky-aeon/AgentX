package org.xhy.domain.conversation.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 标准消息处理器
 */
@Component(value = "standardMessageHandler")
public class StandardMessageHandler implements MessageHandler {
    
    /**
     * 连接超时时间（毫秒）: 5分钟
     */
    private static final long CONNECTION_TIMEOUT = 300000L;
    
    /**
     * 摘要前缀信息
     */
    private static final String SUMMARY_PREFIX = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n";
    
    private final ConversationDomainService conversationDomainService;
    private final ContextDomainService contextDomainService;
    private final LLMServiceFactory llmServiceFactory;
    
    public StandardMessageHandler(
            ConversationDomainService conversationDomainService,
            ContextDomainService contextDomainService,
            LLMServiceFactory llmServiceFactory) {
        this.conversationDomainService = conversationDomainService;
        this.contextDomainService = contextDomainService;
        this.llmServiceFactory = llmServiceFactory;
    }
    
    @Override
    public <T> T handleChat(ChatEnvironment environment, MessageTransport<T> transport) {
        // 创建用户消息实体
        MessageEntity userMessageEntity = createUserMessage(environment);
        
        // 创建LLM消息实体
        MessageEntity llmMessageEntity = createLlmMessage(environment);
        
        // 创建连接
        T connection = transport.createConnection(CONNECTION_TIMEOUT);
        
        // 准备LLM请求
        dev.langchain4j.model.chat.request.ChatRequest llmRequest = prepareLlmRequest(environment);
        
        // 获取LLM客户端
        StreamingChatLanguageModel llmClient = llmServiceFactory.getStreamingClient(
                environment.getProvider(), environment.getModel());
        
        // 处理对话
        processChat(llmClient, llmRequest, connection, transport, environment, 
                  userMessageEntity, llmMessageEntity);
        
        return connection;
    }
    
    /**
     * 创建用户消息实体
     */
    private MessageEntity createUserMessage(ChatEnvironment environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.USER);
        messageEntity.setContent(environment.getUserMessage());
        messageEntity.setSessionId(environment.getSessionId());
        return messageEntity;
    }
    
    /**
     * 创建LLM消息实体
     */
    private MessageEntity createLlmMessage(ChatEnvironment environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.SYSTEM);
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setModel(environment.getModel().getModelId());
        messageEntity.setProvider(environment.getProvider().getId());
        return messageEntity;
    }
    
    /**
     * 准备LLM请求
     */
    private dev.langchain4j.model.chat.request.ChatRequest prepareLlmRequest(ChatEnvironment environment) {
        // 构建聊天消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();
        dev.langchain4j.model.chat.request.ChatRequest.Builder chatRequestBuilder = 
                new dev.langchain4j.model.chat.request.ChatRequest.Builder();
        
        // 1. 首先添加系统提示(如果有)
        if (StringUtils.isNotEmpty(environment.getAgent().getSystemPrompt())) {
            chatMessages.add(new SystemMessage(environment.getAgent().getSystemPrompt()));
        }
        
        // 2. 有条件地添加摘要信息(作为AI消息，但有明确的前缀标识)
        if (StringUtils.isNotEmpty(environment.getContextEntity().getSummary())) {
            // 添加为AI消息，但明确标识这是摘要
            chatMessages.add(new AiMessage(SUMMARY_PREFIX + environment.getContextEntity().getSummary()));
        }
        
        // 3. 添加对话历史
        for (MessageEntity messageEntity : environment.getMessageHistory()) {
            Role role = messageEntity.getRole();
            String content = messageEntity.getContent();
            if (role == Role.USER) {
                chatMessages.add(new UserMessage(content));
            } else if (role == Role.SYSTEM) {
                // 历史中的SYSTEM角色实际上是AI的回复
                chatMessages.add(new AiMessage(content));
            }
        }
        
        // 4. 添加当前用户消息
        chatMessages.add(new UserMessage(environment.getUserMessage()));
        
        // 构建请求参数
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(environment.getModel().getModelId());
        parameters.topP(environment.getLlmModelConfig().getTopP())
                 .temperature(environment.getLlmModelConfig().getTemperature());
        
        // 设置消息和参数
        chatRequestBuilder.messages(chatMessages);
        chatRequestBuilder.parameters(parameters.build());
        
        return chatRequestBuilder.build();
    }
    
    /**
     * 处理对话
     */
    private <T> void processChat(
            StreamingChatLanguageModel llmClient,
            dev.langchain4j.model.chat.request.ChatRequest llmRequest,
            T connection,
            MessageTransport<T> transport,
            ChatEnvironment environment,
            MessageEntity userMessageEntity,
            MessageEntity llmMessageEntity) {
        
        llmClient.doChat(llmRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                transport.sendMessage(
                        connection,
                        partialResponse,
                        false,
                        environment.getProvider().getName(),
                        environment.getModel().getModelId()
                );
            }
            
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                int historyBodyTokenCountSum = 0;
                if (CollectionUtil.isNotEmpty(environment.getMessageHistory())) {
                    historyBodyTokenCountSum = environment.getMessageHistory().stream()
                            .mapToInt(MessageEntity::getBodyTokenCount)
                            .sum();
                }
                // 设置token使用情况
                TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();
                
                // 设置用户消息token数和消息本体token数
                Integer inputTokenCount = tokenUsage.inputTokenCount();
                userMessageEntity.setTokenCount(inputTokenCount);
                userMessageEntity.setBodyTokenCount(inputTokenCount - historyBodyTokenCountSum);
                // 设置LLM消息内容和token数
                Integer outputTokenCount = tokenUsage.outputTokenCount();
                llmMessageEntity.setTokenCount(outputTokenCount);
                llmMessageEntity.setBodyTokenCount(outputTokenCount);
                llmMessageEntity.setContent(completeResponse.aiMessage().text());
                // 发送完成消息
                transport.sendMessage(
                        connection,
                        "",
                        true,
                        environment.getProvider().getName(),
                        environment.getModel().getModelId()
                );
                transport.completeConnection(connection);
                
                // 保存消息
                conversationDomainService.insertBathMessage(Arrays.asList(userMessageEntity, llmMessageEntity));
                
                // 更新上下文
                List<String> activeMessages = environment.getContextEntity().getActiveMessages();
                activeMessages.add(userMessageEntity.getId());
                activeMessages.add(llmMessageEntity.getId());
                contextDomainService.insertOrUpdate(environment.getContextEntity());
            }
            
            @Override
            public void onError(Throwable error) {
                transport.handleError(connection, error);
            }
        });
    }
} 