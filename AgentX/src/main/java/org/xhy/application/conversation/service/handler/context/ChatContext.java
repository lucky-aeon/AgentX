package org.xhy.application.conversation.service.handler.context;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;

import java.util.ArrayList;
import java.util.List;

/** chat 上下文，包含对话所需的所有信息 */
public class ChatContext {
    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private String userId;

    /** 用户消息 */
    private String userMessage;

    /** 智能体实体 */
    private AgentEntity agent;

    /** 模型实体 */
    private ModelEntity model;

    /** 服务商实体 */
    private ProviderEntity provider;

    /** 大模型配置 */
    private LLMModelConfig llmModelConfig;

    /** 上下文实体 */
    private ContextEntity contextEntity;

    /** 历史消息列表 */
    private List<MessageEntity> messageHistory;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public AgentEntity getAgent() {
        return agent;
    }

    public void setAgent(AgentEntity agent) {
        this.agent = agent;
    }

    public ModelEntity getModel() {
        return model;
    }

    public void setModel(ModelEntity model) {
        this.model = model;
    }

    public ProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ProviderEntity provider) {
        this.provider = provider;
    }

    public LLMModelConfig getLlmModelConfig() {
        return llmModelConfig;
    }

    public void setLlmModelConfig(LLMModelConfig llmModelConfig) {
        this.llmModelConfig = llmModelConfig;
    }

    public ContextEntity getContextEntity() {
        return contextEntity;
    }

    public void setContextEntity(ContextEntity contextEntity) {
        this.contextEntity = contextEntity;
    }

    public List<MessageEntity> getMessageHistory() {
        return messageHistory;
    }

    public void setMessageHistory(List<MessageEntity> messageHistory) {
        this.messageHistory = messageHistory;
    }

    public ChatRequest prepareChatRequest() {
        // 构建聊天消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();
        dev.langchain4j.model.chat.request.ChatRequest.Builder chatRequestBuilder = new dev.langchain4j.model.chat.request.ChatRequest.Builder();

        // 1. 首先添加系统提示(如果有)
        if (StringUtils.isNotEmpty(this.getAgent().getSystemPrompt())) {
            chatMessages.add(new SystemMessage(this.getAgent().getSystemPrompt()));
        }

        // 2. 有条件地添加摘要信息(作为AI消息，但有明确的前缀标识)
        if (StringUtils.isNotEmpty(this.getContextEntity().getSummary())) {
            // 添加为AI消息，但明确标识这是摘要
            chatMessages
                    .add(new AiMessage(AgentPromptTemplates.getSummaryPrefix() + this.getContextEntity().getSummary()));
        }

        // 3. 添加对话历史
        for (MessageEntity messageEntity : this.getMessageHistory()) {
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
        chatMessages.add(new UserMessage(this.getUserMessage()));

        // 构建请求参数
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(this.getModel().getModelId());
        parameters.topP(this.getLlmModelConfig().getTopP()).temperature(this.getLlmModelConfig().getTemperature());

        // 设置消息和参数
        chatRequestBuilder.messages(chatMessages);
        chatRequestBuilder.parameters(parameters.build());

        return chatRequestBuilder.build();
    }
}