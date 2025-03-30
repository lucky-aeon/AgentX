package org.xhy.application.conversation.service;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.agent.service.AgentWorkspaceDomainService;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LlmDomainService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.function.BiConsumer;

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

    public ConversationAppService(
            ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService, AgentDomainService agentDomainService, AgentWorkspaceDomainService agentWorkspaceDomainService, LlmDomainService llmDomainService) {
        this.conversationDomainService = conversationDomainService;
        this.sessionDomainService = sessionDomainService;
        this.agentDomainService = agentDomainService;
        this.agentWorkspaceDomainService = agentWorkspaceDomainService;
        this.llmDomainService = llmDomainService;
    }

    /**
     * 处理流式聊天请求
     */
    public void chatStream(StreamChatRequest request, BiConsumer<StreamChatResponse, Boolean> responseHandler) {
        conversationDomainService.chatStream(request, responseHandler);
    }

    public MessageEntity saveAssistantMessage(String sessionId, String content,
            String provider, String model, Integer tokenCount) {
        return conversationDomainService.saveAssistantMessage(sessionId, content, provider, model, tokenCount);
    }

    /**
     * 发送消息 - 保存用户消息并创建或更新上下文
     *
     * @param sessionId 会话id
     * @param userId    用户id
     * @param message   消息内容
     * @param modelName 模型名称
     * @return 保存的用户消息实体
     */
    public MessageEntity sendMessage(String sessionId, String userId, String message, String modelName) {
        return conversationDomainService.sendMessage(sessionId, userId, message, modelName);
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

        if (sessionEntity == null){
            throw new BusinessException("会话不存在");
        }

        List<MessageEntity> conversationMessages = conversationDomainService.getConversationMessages(sessionId);
        return MessageAssembler.toDTOs(conversationMessages);
    }


    public void chat(ChatRequest chatRequest, String userId){

        // 获取会话
        String sessionId = chatRequest.getSessionId();
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        String agentId = session.getAgentId();

        // 获取对应agent是否可以使用：如果 userId 不同并且是禁用，则不可对话
        AgentEntity agent = agentDomainService.getAgentById(agentId);
        if (!agent.getUserId().equals(userId) && !agent.getEnabled()){
            throw new BusinessException("agent已被禁用");
        }

        // 从工作区中获取对应的模型信息
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(agentId, userId);
        String modelId = workspace.getModelId();
        ModelEntity model = llmDomainService.getModelById(modelId);

        model.isActive();

        // 获取服务商信息
        ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
        provider.isActive();

        // 对话

    }

    private void chatStream(){
    }
}