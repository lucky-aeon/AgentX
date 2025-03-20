package org.xhy.domain.conversation.service;

import org.xhy.domain.conversation.model.Message;

import java.util.List;

/**
 * 上下文服务接口
 */
public interface ContextService {

    /**
     * 获取会话上下文(活跃消息)
     */
    List<Message> getContextMessages(String sessionId);

    /**
     * 添加消息到上下文
     */
    void addMessageToContext(String sessionId, String messageId);

    /**
     * 根据策略更新上下文
     * 当上下文消息过多时，应用管理策略
     */
    void updateContext(String sessionId);

    /**
     * 清空上下文
     */
    void clearContext(String sessionId);

    /**
     * 创建会话初始上下文
     */
    void createInitialContext(String sessionId);

    /**
     * 删除会话上下文
     */
    void deleteContext(String sessionId);
}