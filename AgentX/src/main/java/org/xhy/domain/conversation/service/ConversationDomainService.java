package org.xhy.domain.conversation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.apache.ibatis.executor.BatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.repository.ContextRepository;
import org.xhy.domain.conversation.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 对话服务实现
 */
@Service
public class ConversationDomainService {

    private final Logger logger = LoggerFactory.getLogger(ConversationDomainService.class);
    private final MessageRepository messageRepository;
    private final ContextRepository contextRepository;
    private final SessionDomainService sessionDomainService;



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
    public List<MessageEntity> getConversationMessages(String sessionId) {
        return messageRepository.selectList(
                Wrappers.<MessageEntity>lambdaQuery().eq(MessageEntity::getSessionId, sessionId));
    }


   public void insertBathMessage(List<MessageEntity> messages){
       messageRepository.insert(messages);
   }

   public MessageEntity saveMessage(MessageEntity message){
       messageRepository.insert(message);
       return message;
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
     * 删除会话下的消息
     * 
     * @param sessionId 会话id
     */
    public void deleteConversationMessages(String sessionId) {
        messageRepository.checkedDelete(Wrappers.<MessageEntity>lambdaQuery().eq(MessageEntity::getSessionId, sessionId));
    }

    public void deleteConversationMessages(List<String> sessionIds) {
        messageRepository.checkedDelete(Wrappers.<MessageEntity>lambdaQuery().in(MessageEntity::getSessionId, sessionIds));
    }

    /**
     * 更新消息的token数量
     * 
     * @param message 消息实体
     */
    @Transactional
    public void updateMessageTokenCount(MessageEntity message) {
        logger.info("更新消息token数量，消息ID: {}, token数量: {}", message.getId(), message.getTokenCount());
        messageRepository.checkedUpdateById(message);
    }
}