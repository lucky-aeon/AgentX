package org.xhy.domain.conversation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.conversation.model.Context;
import org.xhy.domain.conversation.model.Message;
import org.xhy.domain.conversation.repository.ContextRepository;
import org.xhy.domain.conversation.repository.MessageRepository;
import org.xhy.domain.conversation.service.ContextService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * 上下文服务实现
 */
@Service
public class ContextServiceImpl implements ContextService {

    private final ContextRepository contextRepository;
    private final MessageRepository messageRepository;

    // 默认上下文窗口大小，实际项目中可通过配置文件设置
    private static final int DEFAULT_CONTEXT_SIZE = 10;

    /**
     * 构造函数注入
     */
    public ContextServiceImpl(ContextRepository contextRepository, MessageRepository messageRepository) {
        this.contextRepository = contextRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public List<Message> getContextMessages(String sessionId) {
        Context context = getOrCreateContext(sessionId);
        List<String> messageIds = context.getActiveMessageIds();

        if (messageIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 根据消息ID列表查询消息实体
        List<Message> messages = new ArrayList<>();
        for (String id : messageIds) {
            Message message = messageRepository.selectById(id);
            if (message != null) {
                messages.add(message);
            }
        }

        return messages;
    }

    @Override
    @Transactional
    public void addMessageToContext(String sessionId, String messageId) {
        Context context = getOrCreateContext(sessionId);
        context.addMessage(messageId);
        context.setUpdatedAt(LocalDateTime.now());
        contextRepository.updateById(context);

        // 添加消息后检查是否需要应用上下文管理策略
        updateContext(sessionId);
    }

    @Override
    @Transactional
    public void updateContext(String sessionId) {
        Context context = getOrCreateContext(sessionId);
        List<String> activeIds = context.getActiveMessageIds();

        // 如果活跃消息数量超过限制，应用滑动窗口策略
        if (activeIds.size() > DEFAULT_CONTEXT_SIZE) {
            // 保留最新的N条消息
            List<String> newActiveIds = activeIds.subList(
                    activeIds.size() - DEFAULT_CONTEXT_SIZE,
                    activeIds.size());
            context.setActiveMessageIds(newActiveIds);
            context.setUpdatedAt(LocalDateTime.now());
            contextRepository.updateById(context);
        }
    }

    @Override
    @Transactional
    public void clearContext(String sessionId) {
        Context context = getOrCreateContext(sessionId);
        context.clear();
        contextRepository.updateById(context);
    }

    @Override
    @Transactional
    public void createInitialContext(String sessionId) {
        // 使用LambdaQueryWrapper查询
        LambdaQueryWrapper<Context> wrapper = Wrappers.<Context>lambdaQuery()
                .eq(Context::getSessionId, sessionId);
        Context existingContext = contextRepository.selectOne(wrapper);

        if (existingContext == null) {
            Context newContext = Context.createNew(sessionId);
            contextRepository.insert(newContext);
        }
    }

    @Override
    @Transactional
    public void deleteContext(String sessionId) {
        LambdaQueryWrapper<Context> wrapper = Wrappers.<Context>lambdaQuery()
                .eq(Context::getSessionId, sessionId);
        contextRepository.delete(wrapper);
    }

    /**
     * 获取或创建上下文
     */
    private Context getOrCreateContext(String sessionId) {
        LambdaQueryWrapper<Context> wrapper = Wrappers.<Context>lambdaQuery()
                .eq(Context::getSessionId, sessionId);
        Context context = contextRepository.selectOne(wrapper);

        if (context == null) {
            Context newContext = Context.createNew(sessionId);
            contextRepository.insert(newContext);
            return newContext;
        }

        return context;
    }
}