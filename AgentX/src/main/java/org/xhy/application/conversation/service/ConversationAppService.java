package org.xhy.application.conversation.service;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.domain.agent.dto.AgentDTO;
import org.xhy.domain.agent.dto.AgentVersionDTO;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.conversation.dto.MessageDTO;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.token.model.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.service.TokenDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.conversation.ConversationRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.ArrayList;

/**
 * 对话应用服务，用于适配域层的对话服务
 */
@Service
public class ConversationAppService {

    private final Logger logger = LoggerFactory.getLogger(ConversationAppService.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConversationDomainService conversationDomainService;
    private final SessionDomainService sessionDomainService;
    private final TokenDomainService tokenDomainService;
    private final AgentDomainService agentDomainService;

    public ConversationAppService(
            ConversationDomainService conversationDomainService,
            SessionDomainService sessionDomainService,
            TokenDomainService tokenDomainService,
            AgentDomainService agentDomainService) {
        this.conversationDomainService = conversationDomainService;
        this.sessionDomainService = sessionDomainService;
        this.tokenDomainService = tokenDomainService;
        this.agentDomainService = agentDomainService;
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

        return conversationDomainService.getConversationMessages(sessionId);
    }
}