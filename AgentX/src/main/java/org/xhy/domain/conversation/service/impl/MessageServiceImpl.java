package org.xhy.domain.conversation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.common.exception.EntityNotFoundException;
import org.xhy.domain.conversation.model.Message;
import org.xhy.domain.conversation.model.MessageDTO;
import org.xhy.domain.conversation.model.Session;
import org.xhy.domain.conversation.repository.MessageRepository;
import org.xhy.domain.conversation.repository.SessionRepository;
import org.xhy.domain.conversation.service.ContextService;
import org.xhy.domain.conversation.service.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * 消息服务实现
 */
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;
    private final ContextService contextService;

    /**
     * 构造函数注入
     */
    public MessageServiceImpl(MessageRepository messageRepository,
            SessionRepository sessionRepository,
            ContextService contextService) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.contextService = contextService;
    }

    @Override
    @Transactional
    public MessageDTO sendUserMessage(String sessionId, String content) {
        // 检查会话是否存在
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        // 创建并保存用户消息
        Message message = Message.createUserMessage(sessionId, content);
        messageRepository.insert(message);

        // 更新会话最后更新时间
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.updateById(session);

        // 将消息添加到上下文
        contextService.addMessageToContext(sessionId, message.getId());

        return message.toDTO();
    }

    @Override
    @Transactional
    public MessageDTO saveAssistantMessage(String sessionId, String content, String provider, String model,
            Integer tokenCount) {
        // 检查会话是否存在
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        // 创建并保存助手消息
        Message message = Message.createAssistantMessage(sessionId, content, provider, model, tokenCount);
        messageRepository.insert(message);

        // 更新会话最后更新时间
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.updateById(session);

        // 将消息添加到上下文
        contextService.addMessageToContext(sessionId, message.getId());

        return message.toDTO();
    }

    @Override
    @Transactional
    public MessageDTO saveSystemMessage(String sessionId, String content) {
        // 检查会话是否存在
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        // 创建并保存系统消息
        Message message = Message.createSystemMessage(sessionId, content);
        messageRepository.insert(message);

        // 将消息添加到上下文
        contextService.addMessageToContext(sessionId, message.getId());

        return message.toDTO();
    }

    @Override
    public List<MessageDTO> getSessionMessages(String sessionId) {
        // 检查会话是否存在
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        // 使用LambdaQueryWrapper获取会话所有消息并按创建时间排序
        LambdaQueryWrapper<Message> queryWrapper = Wrappers.<Message>lambdaQuery()
                .eq(Message::getSessionId, sessionId)
                .orderByAsc(Message::getCreatedAt);
        
        List<Message> messages = messageRepository.selectList(queryWrapper);
        return messages.stream()
                .map(Message::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getRecentMessages(String sessionId, int count) {
        // 检查会话是否存在
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        // 使用LambdaQueryWrapper获取会话最近的N条消息，使用last方法限制条数
        LambdaQueryWrapper<Message> queryWrapper = Wrappers.<Message>lambdaQuery()
                .eq(Message::getSessionId, sessionId)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT " + count);
        
        List<Message> recentMessages = messageRepository.selectList(queryWrapper);

        // 我们需要将结果反转为按时间正序
        return recentMessages.stream()
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(Message::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMessage(String messageId) {
        messageRepository.deleteById(messageId);
    }

    @Override
    @Transactional
    public void deleteSessionMessages(String sessionId) {
        // 使用LambdaQueryWrapper删除指定会话的所有消息
        LambdaQueryWrapper<Message> wrapper = Wrappers.<Message>lambdaQuery()
                .eq(Message::getSessionId, sessionId);
        messageRepository.delete(wrapper);
    }
}