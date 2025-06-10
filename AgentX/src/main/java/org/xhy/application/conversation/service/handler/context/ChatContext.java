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

    /** 使用的 mcp server name */
    private List<String> mcpServerNames;

    /** 多模态的文件 */
    private List<String> fileUrls;

    /** 高可用实例ID */
    private String instanceId;

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

    public List<String> getMcpServerNames() {
        return mcpServerNames;
    }

    public void setMcpServerNames(List<String> mcpServerNames) {
        this.mcpServerNames = mcpServerNames;
    }

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}