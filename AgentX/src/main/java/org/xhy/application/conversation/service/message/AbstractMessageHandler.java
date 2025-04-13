package org.xhy.application.conversation.service.message;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractMessageHandler {

    /**
     * 连接超时时间（毫秒）
     */
    protected static final long CONNECTION_TIMEOUT = 3000000L;

    /**
     * 摘要前缀信息
     */
    protected static final String SUMMARY_PREFIX = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n";


    protected final ConversationDomainService conversationDomainService;
    protected final ContextDomainService contextDomainService;
    protected final LLMServiceFactory llmServiceFactory;


    public AbstractMessageHandler(
            ConversationDomainService conversationDomainService,
            ContextDomainService contextDomainService,
            LLMServiceFactory llmServiceFactory) {
        this.conversationDomainService = conversationDomainService;
        this.contextDomainService = contextDomainService;
        this.llmServiceFactory = llmServiceFactory;
    }

    /**
     * 处理对话
     *
     * @param environment 对话环境
     * @param transport 消息传输实现
     * @return 连接对象
     * @param <T> 连接类型
     */
    public abstract <T> T chat(ChatContext environment, MessageTransport<T> transport);


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
     * 准备LLM请求
     */
    protected dev.langchain4j.model.chat.request.ChatRequest prepareChatRequest(ChatContext environment) {
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

   protected void saveMessages(ChatContext chatContext,MessageEntity userMessageEntity,MessageEntity llmMessageEntity){
       // 保存消息
       conversationDomainService.insertBathMessage(Arrays.asList(userMessageEntity, llmMessageEntity));

       // 更新上下文
       List<String> activeMessages = chatContext.getContextEntity().getActiveMessages();
       activeMessages.add(userMessageEntity.getId());
       activeMessages.add(llmMessageEntity.getId());
       contextDomainService.insertOrUpdate(chatContext.getContextEntity());
   }
}
