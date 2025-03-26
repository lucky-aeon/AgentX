package org.xhy.domain.conversation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.conversation.assembler.MessageAssembler;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.domain.conversation.dto.MessageDTO;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.repository.ContextRepository;
import org.xhy.domain.conversation.repository.MessageRepository;
import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.model.LlmResponse;
import org.xhy.domain.llm.service.LlmService;
import org.xhy.infrastructure.integration.llm.siliconflow.SiliconFlowLlmService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 对话服务实现
 */
@Service
public class ConversationDomainService {

    private final Logger logger = LoggerFactory.getLogger(ConversationDomainService.class);
    private final MessageRepository messageRepository;
    private final ContextRepository contextRepository;
    private final SessionDomainService sessionDomainService;

    @Resource
    private LlmService defaultLlmService;

    @Resource
    private Map<String, LlmService> llmServiceMap;


    public ConversationDomainService(MessageRepository messageRepository,
            ContextRepository contextRepository,
            SessionDomainService sessionDomainService) {
        this.messageRepository = messageRepository;
        this.contextRepository = contextRepository;
        this.sessionDomainService = sessionDomainService;
    }



    /**
     * 获取会话中的消息列表
     *
     * @param sessionId 会话id
     * @return 消息列表
     */
    public List<MessageDTO> getConversationMessages(String sessionId) {
        List<MessageEntity> messageEntities = messageRepository.selectList(
                Wrappers.<MessageEntity>lambdaQuery().eq(MessageEntity::getSessionId, sessionId));
        return messageEntities.stream().map(MessageAssembler::toDTO).collect(Collectors.toList());
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
    @Transactional
    public MessageEntity sendMessage(String sessionId, String userId, String message, String modelName) {
        logger.info("保存用户消息，会话ID: {}, 用户ID: {}", sessionId, userId);

        // 检查会话是否存在
        sessionDomainService.checkSessionExist(sessionId, userId);

        // 创建并保存用户消息
        MessageEntity userMessage = MessageEntity.createUserMessage(sessionId, message);
        messageRepository.insert(userMessage);

        // 更新上下文，添加新消息
//        updateContext(sessionId, userMessage.getId());

        return userMessage;
    }

    /**
     * 保存助理回复消息 todo xhy 暂时这样写，后续不需要传其他信息，用其他表保存
     * 
     * @param sessionId  会话id
     * @param content    消息内容
     * @param provider   服务提供商
     * @param model      模型名称
     * @param tokenCount token数量
     * @return 保存的助理消息实体
     *
     */
    @Transactional
    public MessageEntity saveAssistantMessage(String sessionId, String content,
            String provider, String model, Integer tokenCount) {
        logger.info("保存助理回复消息，会话ID: {}", sessionId);

        // 创建并保存助理消息
        MessageEntity assistantMessage = MessageEntity.createAssistantMessage(
                sessionId, content, provider, model, tokenCount);
        messageRepository.insert(assistantMessage);
        return assistantMessage;
    }

    /**
     * 更新上下文，添加新消息到活跃消息列表
     * 
     * @param sessionId 会话id
     * @param messageId 消息id
     */
    private void updateContext(String sessionId, String messageId) {
        // 查找当前会话的上下文
        ContextEntity context = contextRepository.selectOne(
                Wrappers.<ContextEntity>lambdaQuery().eq(ContextEntity::getSessionId, sessionId));

        // 如果上下文不存在，创建新上下文
        if (context == null) {
            context = ContextEntity.createNew(sessionId);
            context.addMessage(messageId);
            contextRepository.insert(context);
        } else {
            // 更新现有上下文
            context.addMessage(messageId);
            context.setUpdatedAt(LocalDateTime.now());
            contextRepository.updateById(context);
        }
    }

    /**
     * 处理普通聊天请求
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        logger.info("接收到聊天请求: {}", request.getMessage());

        LlmService llmService = getLlmService(request.getProvider());

        LlmRequest llmRequest = new LlmRequest();
        llmRequest.addUserMessage(request.getMessage());

        if (request.getModel() != null && !request.getModel().isEmpty()) {
            logger.info("用户指定模型: {}", request.getModel());
            llmRequest.setModel(request.getModel());
        } else {
            logger.info("使用默认模型: {}", llmService.getDefaultModel());
        }

        LlmResponse llmResponse = llmService.chat(llmRequest);

        ChatResponse response = new ChatResponse();
        response.setContent(llmResponse.getContent());
        response.setProvider(llmResponse.getProvider());
        response.setModel(llmResponse.getModel());
        response.setSessionId(request.getSessionId());

        return response;
    }



    /**
     * 获取对应的LLM服务
     *
     * @param provider 服务商名称
     * @return LLM服务
     */
    private LlmService getLlmService(String provider) {
        if (provider == null || provider.isEmpty()) {
            logger.info("使用默认LLM服务: {}", defaultLlmService.getProviderName());
            return defaultLlmService;
        }

        String serviceName = provider.toLowerCase() + "LlmService";
        logger.debug("尝试获取服务: {}", serviceName);

        LlmService service = llmServiceMap.get(serviceName);

        if (service == null) {
            logger.warn("未找到服务商 [{}] 的实现，使用默认服务商: {}", provider, defaultLlmService.getProviderName());
            return defaultLlmService;
        }

        logger.info("使用服务商: {}", service.getProviderName());
        return service;
    }

    /**
     * 处理流式聊天请求，使用回调处理响应
     *
     * @param request         流式聊天请求
     * @param responseHandler 响应处理回调
     */
    public void chatStream(StreamChatRequest request, BiConsumer<StreamChatResponse, Boolean> responseHandler) {
        logger.info("接收到真实流式聊天请求: {}", request.getMessage());

        LlmService llmService = getLlmService(request.getProvider());

        LlmRequest llmRequest = new LlmRequest();
        llmRequest.addUserMessage(request.getMessage());

        // 确保设置流式参数为true
        llmRequest.setStream(true);

        if (request.getModel() != null && !request.getModel().isEmpty()) {
            logger.info("用户指定模型: {}", request.getModel());
            llmRequest.setModel(request.getModel());
        } else {
            logger.info("使用默认模型: {}", llmService.getDefaultModel());
        }

        try {
            // 检查LLM服务是否为SiliconFlowLlmService以使用其回调接口
            if (llmService instanceof SiliconFlowLlmService) {
                logger.info("使用SiliconFlow的真实流式响应");
                SiliconFlowLlmService siliconFlowService = (SiliconFlowLlmService) llmService;

                // 使用回调接口
                siliconFlowService.streamChat(llmRequest, (chunk, isLast) -> {
                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent(chunk);
                    response.setDone(isLast);
                    response.setProvider(llmService.getProviderName());
                    response.setModel(
                            llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
                    response.setSessionId(request.getSessionId());

                    // 调用响应处理回调
                    responseHandler.accept(response, isLast);
                });
            } else {
                // 对于不支持回调的LLM服务，使用原来的方式
                logger.info("服务商不支持真实流式，使用传统分块方式");
                List<String> chunks = llmService.chatStreamList(llmRequest);

                // 转换为流式响应
                for (int i = 0; i < chunks.size(); i++) {
                    boolean isLast = (i == chunks.size() - 1);

                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent(chunks.get(i));
                    response.setDone(isLast);
                    response.setProvider(llmService.getProviderName());
                    response.setModel(
                            llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
                    response.setSessionId(request.getSessionId());

                    // 调用响应处理回调
                    responseHandler.accept(response, isLast);
                }
            }

        } catch (Exception e) {
            logger.error("处理流式聊天请求异常", e);
            // 发生异常时，返回一个错误响应
            StreamChatResponse errorResponse = new StreamChatResponse();
            errorResponse.setContent("处理请求时发生错误: " + e.getMessage());
            errorResponse.setDone(true);
            errorResponse.setProvider(llmService.getProviderName());
            errorResponse
                    .setModel(llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
            errorResponse.setSessionId(request.getSessionId());

            // 调用响应处理回调，并标记为最后一个
            responseHandler.accept(errorResponse, true);
        }
    }

    /**
     * 删除会话下的消息
     * 
     * @param sessionId 会话id
     */
    public void deleteConversationMessages(String sessionId) {
        messageRepository.delete(Wrappers.<MessageEntity>lambdaQuery().eq(MessageEntity::getSessionId, sessionId));
    }

    public void deleteConversationMessages(List<String> sessionIds) {
        messageRepository.delete(Wrappers.<MessageEntity>lambdaQuery().in(MessageEntity::getSessionId, sessionIds));
    }

    /**
     * 更新消息的token数量
     * 
     * @param message 消息实体
     */
    @Transactional
    public void updateMessageTokenCount(MessageEntity message) {
        logger.info("更新消息token数量，消息ID: {}, token数量: {}", message.getId(), message.getTokenCount());
        messageRepository.updateById(message);
    }
}